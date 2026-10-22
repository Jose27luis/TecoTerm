package pe.amantix.tecoterm.core.ssh.connection

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.amantix.tecoterm.core.ssh.ChannelOpenFailedException
import pe.amantix.tecoterm.core.ssh.ChannelRequestFailedException
import pe.amantix.tecoterm.core.ssh.SshException
import pe.amantix.tecoterm.core.ssh.SshProtocolException
import pe.amantix.tecoterm.core.ssh.transport.IncomingPacket
import pe.amantix.tecoterm.core.ssh.transport.Transport
import pe.amantix.tecoterm.core.ssh.wire.SshMessage
import pe.amantix.tecoterm.core.ssh.wire.SshReader
import pe.amantix.tecoterm.core.ssh.wire.SshWriter
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

public class SessionChannel internal constructor(
    internal val idLocal: Int,
    private val transport: Transport,
    private val alLiberar: (Int) -> Unit,
) {
    private val ventanaLocal: LocalWindow = LocalWindow(VENTANA_INICIAL.toLong())
    private val ventanaRemota: RemoteWindow = RemoteWindow()
    private val apertura: CompletableDeferred<Unit> = CompletableDeferred()
    private val respuestasPendientes: ConcurrentLinkedQueue<CompletableDeferred<Boolean>> = ConcurrentLinkedQueue()
    private val peticiones: Mutex = Mutex()
    private val escritura: Mutex = Mutex()
    private val cierreEnviado: AtomicBoolean = AtomicBoolean(false)
    private val estado: CompletableDeferred<ExitStatus> = CompletableDeferred()

    @Volatile
    private var idRemoto: Int = 0

    @Volatile
    private var paqueteMaximoRemoto: Int = 0

    public val salida: ChannelReader = ChannelReader(::consumir)
    public val errores: ChannelReader = ChannelReader(::consumir)
    public val estadoSalida: Deferred<ExitStatus> = estado

    public suspend fun escribir(datos: ByteArray) {
        escritura.withLock {
            var posicion = 0
            while (posicion < datos.size) {
                val cantidad = ventanaRemota.reservar(minOf(datos.size - posicion, paqueteMaximoRemoto))
                transport.enviar(
                    SshWriter(cantidad + CABECERA_DATOS)
                        .escribirByte(SshMessage.SSH_MSG_CHANNEL_DATA)
                        .escribirUint32(idRemoto)
                        .escribirString(datos.copyOfRange(posicion, posicion + cantidad))
                        .bytes(),
                )
                posicion += cantidad
            }
        }
    }

    public suspend fun cambiarTamano(
        columnas: Int,
        filas: Int,
        anchoPixeles: Int = 0,
        altoPixeles: Int = 0,
    ) {
        peticion("window-change", conRespuesta = false) {
            escribirUint32(columnas).escribirUint32(filas).escribirUint32(anchoPixeles).escribirUint32(altoPixeles)
        }
    }

    public suspend fun enviarSenal(nombre: String) {
        peticion("signal", conRespuesta = false) { escribirTexto(nombre) }
    }

    public suspend fun enviarEof() {
        transport.enviar(SshWriter().escribirByte(SshMessage.SSH_MSG_CHANNEL_EOF).escribirUint32(idRemoto).bytes())
    }

    public suspend fun cerrar() {
        if (cierreEnviado.compareAndSet(false, true)) {
            transport.enviar(SshWriter().escribirByte(SshMessage.SSH_MSG_CHANNEL_CLOSE).escribirUint32(idRemoto).bytes())
        }
    }

    internal suspend fun abrir() {
        transport.enviar(
            SshWriter()
                .escribirByte(SshMessage.SSH_MSG_CHANNEL_OPEN)
                .escribirTexto(TIPO_SESION)
                .escribirUint32(idLocal)
                .escribirUint32(VENTANA_INICIAL)
                .escribirUint32(PAQUETE_MAXIMO_LOCAL)
                .bytes(),
        )
        apertura.await()
    }

    internal suspend fun solicitarPty(pty: PtyRequest) {
        peticion("pty-req", conRespuesta = true) {
            escribirTexto(pty.termino)
                .escribirUint32(pty.columnas)
                .escribirUint32(pty.filas)
                .escribirUint32(pty.anchoPixeles)
                .escribirUint32(pty.altoPixeles)
                .escribirString(modosTerminal)
        }
    }

    internal suspend fun iniciarShell() {
        peticion("shell", conRespuesta = true) {}
    }

    internal suspend fun ejecutar(comando: String) {
        peticion("exec", conRespuesta = true) { escribirTexto(comando) }
    }

    internal suspend fun recibirMensaje(paquete: IncomingPacket) {
        val lector = SshReader(paquete.payload)
        lector.leerByte()
        lector.leerUint32()
        when (paquete.tipo) {
            SshMessage.SSH_MSG_CHANNEL_OPEN_CONFIRMATION -> confirmarApertura(lector)
            SshMessage.SSH_MSG_CHANNEL_OPEN_FAILURE -> rechazarApertura(lector)
            SshMessage.SSH_MSG_CHANNEL_WINDOW_ADJUST -> ventanaRemota.agregar(lector.leerUint32())
            SshMessage.SSH_MSG_CHANNEL_DATA -> recibirDatos(salida, lector.leerString())
            SshMessage.SSH_MSG_CHANNEL_EXTENDED_DATA -> recibirDatosExtendidos(lector)
            SshMessage.SSH_MSG_CHANNEL_EOF -> finalizarLectores(null)
            SshMessage.SSH_MSG_CHANNEL_CLOSE -> recibirCierre()
            SshMessage.SSH_MSG_CHANNEL_REQUEST -> recibirPeticion(lector)
            SshMessage.SSH_MSG_CHANNEL_SUCCESS, SshMessage.SSH_MSG_CHANNEL_FAILURE -> responderPeticion(paquete.tipo)
            else -> throw SshProtocolException("Mensaje de canal desconocido: ${paquete.tipo}")
        }
    }

    internal fun terminar(causa: SshException?) {
        ventanaRemota.cerrar()
        finalizarLectores(causa)
        apertura.completeExceptionally(causa ?: ChannelOpenFailedException(0, "El canal se cerró"))
        respuestasPendientes.forEach { it.complete(false) }
        respuestasPendientes.clear()
        estado.complete(ExitStatus.SinEstado)
    }

    private suspend fun peticion(
        nombre: String,
        conRespuesta: Boolean,
        datos: SshWriter.() -> Unit,
    ) {
        val respuesta = if (conRespuesta) CompletableDeferred<Boolean>() else null
        peticiones.withLock {
            respuesta?.let(respuestasPendientes::add)
            val escritor =
                SshWriter()
                    .escribirByte(SshMessage.SSH_MSG_CHANNEL_REQUEST)
                    .escribirUint32(idRemoto)
                    .escribirTexto(nombre)
                    .escribirBoolean(conRespuesta)
            escritor.datos()
            transport.enviar(escritor.bytes())
        }
        if (respuesta != null && !respuesta.await()) {
            throw ChannelRequestFailedException(nombre)
        }
    }

    private fun confirmarApertura(lector: SshReader) {
        idRemoto = lector.leerUint32().toInt()
        ventanaRemota.agregar(lector.leerUint32())
        paqueteMaximoRemoto = lector.leerUint32().coerceIn(1, PAQUETE_MAXIMO_LOCAL.toLong()).toInt()
        apertura.complete(Unit)
    }

    private fun rechazarApertura(lector: SshReader) {
        val codigo = lector.leerUint32().toInt()
        val descripcion = lector.leerTexto()
        apertura.completeExceptionally(ChannelOpenFailedException(codigo, descripcion))
        alLiberar(idLocal)
    }

    private fun recibirDatos(
        lectorDestino: ChannelReader,
        datos: ByteArray,
    ) {
        if (!ventanaLocal.recibir(datos.size)) {
            throw SshProtocolException("El servidor excedió la ventana del canal")
        }
        lectorDestino.entregar(datos)
    }

    private suspend fun recibirDatosExtendidos(lector: SshReader) {
        val tipo = lector.leerUint32()
        val datos = lector.leerString()
        if (tipo == SshMessage.SSH_EXTENDED_DATA_STDERR.toLong()) {
            recibirDatos(errores, datos)
        } else {
            if (!ventanaLocal.recibir(datos.size)) {
                throw SshProtocolException("El servidor excedió la ventana del canal")
            }
            consumir(datos.size)
        }
    }

    private suspend fun recibirCierre() {
        cerrar()
        terminar(null)
        alLiberar(idLocal)
    }

    private suspend fun recibirPeticion(lector: SshReader) {
        val nombre = lector.leerTexto()
        val quiereRespuesta = lector.leerBoolean()
        when (nombre) {
            "exit-status" -> {
                estado.complete(ExitStatus.Codigo(lector.leerUint32().toInt()))
            }

            "exit-signal" -> {
                val senal = lector.leerTexto()
                lector.leerBoolean()
                estado.complete(ExitStatus.Senal(senal, lector.leerTexto()))
            }

            else -> {
                if (quiereRespuesta) {
                    transport.enviar(SshWriter().escribirByte(SshMessage.SSH_MSG_CHANNEL_FAILURE).escribirUint32(idRemoto).bytes())
                }
            }
        }
    }

    private fun responderPeticion(tipo: Int) {
        val pendiente = respuestasPendientes.poll() ?: throw SshProtocolException("Respuesta a una petición de canal no realizada")
        pendiente.complete(tipo == SshMessage.SSH_MSG_CHANNEL_SUCCESS)
    }

    private fun finalizarLectores(causa: SshException?) {
        salida.finalizar(causa)
        errores.finalizar(causa)
    }

    private suspend fun consumir(bytes: Int) {
        val ajuste = ventanaLocal.consumir(bytes) ?: return
        if (!cierreEnviado.get()) {
            transport.enviar(
                SshWriter()
                    .escribirByte(SshMessage.SSH_MSG_CHANNEL_WINDOW_ADJUST)
                    .escribirUint32(idRemoto)
                    .escribirUint32(ajuste.toInt())
                    .bytes(),
            )
        }
    }

    private companion object {
        const val TIPO_SESION: String = "session"
        const val VENTANA_INICIAL: Int = 2 * 1024 * 1024
        const val PAQUETE_MAXIMO_LOCAL: Int = 32 * 1024
        const val CABECERA_DATOS: Int = 16
        const val TTY_OP_END: Int = 0
        const val VERASE: Int = 3
        const val IUTF8: Int = 42
        const val CODIGO_SUPRIMIR: Int = 127

        val modosTerminal: ByteArray =
            SshWriter()
                .escribirByte(VERASE)
                .escribirUint32(CODIGO_SUPRIMIR)
                .escribirByte(IUTF8)
                .escribirUint32(1)
                .escribirByte(TTY_OP_END)
                .bytes()
    }
}
