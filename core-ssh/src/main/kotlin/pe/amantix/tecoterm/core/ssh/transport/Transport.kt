package pe.amantix.tecoterm.core.ssh.transport

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.amantix.tecoterm.core.ssh.ConnectionClosedException
import pe.amantix.tecoterm.core.ssh.SshException
import pe.amantix.tecoterm.core.ssh.SshProtocolException
import pe.amantix.tecoterm.core.ssh.transport.cipher.PacketCipherFactory
import pe.amantix.tecoterm.core.ssh.wire.SshMessage
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.nanoseconds

internal class Transport(
    private val canal: PacketChannel,
    versionServidor: String,
    private val opciones: TransportOptions,
    private val alcance: CoroutineScope,
    private val aleatorio: SecureRandom,
    private val alCerrar: () -> Unit,
) {
    private val escritura: Mutex = Mutex()
    private val enIntercambio: MutableStateFlow<Boolean> = MutableStateFlow(true)
    private val entrantes: Channel<IncomingPacket> = Channel(Channel.UNLIMITED)
    private val primerIntercambio: CompletableDeferred<Unit> = CompletableDeferred()
    private val causaCierre: AtomicReference<SshException?> = AtomicReference(null)
    private val negociador: AlgorithmNegotiator = AlgorithmNegotiator(opciones.algoritmos)
    private val proceso: KeyExchangeProcess =
        KeyExchangeProcess(canal, escritura, negociador, PacketCipherFactory(aleatorio), opciones, versionServidor)

    @Volatile
    private var kexInitEnviado: KexInit? = null

    @Volatile
    private var identidad: SessionIdentity? = null

    @Volatile
    private var inicioUltimoIntercambio: Long = System.nanoTime()

    @Volatile
    var algoritmosFirmaServidor: Set<String> = emptySet()
        private set

    @Volatile
    var intercambiosRealizados: Int = 0
        private set

    val idSesion: ByteArray
        get() = identidad?.idSesion?.copyOf() ?: throw SshProtocolException("El intercambio de claves no terminó")

    suspend fun iniciar() {
        alcance.launch { leerContinuamente() }
        enviarKexInit()
        primerIntercambio.await()
    }

    suspend fun enviar(payload: ByteArray) {
        while (true) {
            causaCierre.get()?.let { throw it }
            enIntercambio.first { !it }
            val enviado =
                escritura.withLock {
                    causaCierre.get()?.let { throw it }
                    if (enIntercambio.value) {
                        false
                    } else {
                        canal.escribir(payload)
                        true
                    }
                }
            if (enviado) {
                revisarRekey()
                return
            }
        }
    }

    suspend fun recibir(): IncomingPacket = entrantes.receive()

    suspend fun desconectar(
        codigo: Int,
        descripcion: String,
    ) {
        try {
            escritura.withLock {
                if (causaCierre.get() == null) {
                    canal.escribir(TransportMessages.crearDesconexion(codigo, descripcion))
                }
            }
        } catch (_: SshException) {
        } finally {
            cerrar(ConnectionClosedException())
        }
    }

    fun cerrar(causa: SshException) {
        if (!causaCierre.compareAndSet(null, causa)) {
            return
        }
        enIntercambio.value = false
        entrantes.close(causa)
        primerIntercambio.completeExceptionally(causa)
        alCerrar()
    }

    private suspend fun leerContinuamente() {
        try {
            while (causaCierre.get() == null) {
                procesar(canal.leer())
            }
        } catch (error: SshException) {
            cerrar(error)
        }
    }

    private suspend fun procesar(paquete: IncomingPacket) {
        if (paquete.tipo in mensajesIgnorados) {
            return
        }
        when (paquete.tipo) {
            SshMessage.SSH_MSG_DISCONNECT -> {
                throw TransportMessages.desconexion(paquete.payload)
            }

            SshMessage.SSH_MSG_KEXINIT -> {
                intercambiar(paquete)
            }

            SshMessage.SSH_MSG_EXT_INFO -> {
                TransportMessages.algoritmosFirmaServidor(paquete.payload)?.let { algoritmosFirmaServidor = it }
            }

            else -> {
                val inesperado = paquete.tipo != SshMessage.SSH_MSG_SERVICE_ACCEPT && SshMessage.esDeTransporte(paquete.tipo)
                if (inesperado || !primerIntercambio.isCompleted) {
                    throw SshProtocolException("Mensaje ${paquete.tipo} inesperado en el transporte")
                }
                entrantes.send(paquete)
                revisarRekey()
            }
        }
    }

    private suspend fun intercambiar(paquete: IncomingPacket) {
        val propio = enviarKexInit()
        identidad = proceso.ejecutar(propio, paquete, identidad)
        escritura.withLock { kexInitEnviado = null }
        canal.reiniciarContador()
        inicioUltimoIntercambio = System.nanoTime()
        intercambiosRealizados++
        enIntercambio.value = false
        primerIntercambio.complete(Unit)
    }

    private suspend fun enviarKexInit(): KexInit =
        escritura.withLock {
            kexInitEnviado ?: crearKexInit()
        }

    private fun crearKexInit(): KexInit {
        val cookie = ByteArray(KexInit.LONGITUD_COOKIE).also(aleatorio::nextBytes)
        val propuesta = KexInit.crear(cookie, negociador.propuesta(primerIntercambio = identidad == null))
        enIntercambio.value = true
        canal.escribir(propuesta.payload)
        kexInitEnviado = propuesta
        return propuesta
    }

    private fun revisarRekey() {
        if (necesitaRekey()) {
            alcance.launch {
                escritura.withLock {
                    if (necesitaRekey() && causaCierre.get() == null) {
                        crearKexInit()
                    }
                }
            }
        }
    }

    private fun necesitaRekey(): Boolean {
        if (identidad == null || kexInitEnviado != null || enIntercambio.value) {
            return false
        }
        val transcurrido = (System.nanoTime() - inicioUltimoIntercambio).nanoseconds
        return canal.bytesDesdeIntercambio >= opciones.limiteBytesRekey || transcurrido >= opciones.limiteTiempoRekey
    }

    private companion object {
        val mensajesIgnorados: Set<Int> =
            setOf(SshMessage.SSH_MSG_IGNORE, SshMessage.SSH_MSG_DEBUG, SshMessage.SSH_MSG_UNIMPLEMENTED)
    }
}
