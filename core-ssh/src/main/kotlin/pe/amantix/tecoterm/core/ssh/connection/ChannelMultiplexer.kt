package pe.amantix.tecoterm.core.ssh.connection

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.amantix.tecoterm.core.ssh.SshException
import pe.amantix.tecoterm.core.ssh.SshProtocolException
import pe.amantix.tecoterm.core.ssh.transport.IncomingPacket
import pe.amantix.tecoterm.core.ssh.transport.Transport
import pe.amantix.tecoterm.core.ssh.wire.SshMessage
import pe.amantix.tecoterm.core.ssh.wire.SshReader
import pe.amantix.tecoterm.core.ssh.wire.SshWriter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

internal class ChannelMultiplexer(
    private val transport: Transport,
    private val alTerminar: (SshException) -> Unit,
) {
    private val canales: ConcurrentHashMap<Int, SessionChannel> = ConcurrentHashMap()
    private val siguienteId: AtomicInteger = AtomicInteger(0)
    private val peticionesGlobales: ConcurrentLinkedQueue<CompletableDeferred<Boolean>> = ConcurrentLinkedQueue()
    private val envioGlobal: Mutex = Mutex()

    fun iniciar(alcance: CoroutineScope): Job = alcance.launch { despachar() }

    suspend fun abrirSesion(): SessionChannel {
        val canal = SessionChannel(siguienteId.getAndIncrement(), transport, ::liberar)
        canales[canal.idLocal] = canal
        try {
            canal.abrir()
        } catch (error: SshException) {
            canales.remove(canal.idLocal)
            throw error
        }
        return canal
    }

    suspend fun peticionGlobal(nombre: String): Boolean {
        val respuesta = CompletableDeferred<Boolean>()
        envioGlobal.withLock {
            peticionesGlobales.add(respuesta)
            transport.enviar(
                SshWriter()
                    .escribirByte(SshMessage.SSH_MSG_GLOBAL_REQUEST)
                    .escribirTexto(nombre)
                    .escribirBoolean(true)
                    .bytes(),
            )
        }
        return respuesta.await()
    }

    private suspend fun despachar() {
        try {
            while (true) {
                enrutar(transport.recibir())
            }
        } catch (error: SshException) {
            canales.values.forEach { it.terminar(error) }
            canales.clear()
            peticionesGlobales.forEach { it.completeExceptionally(error) }
            peticionesGlobales.clear()
            alTerminar(error)
        }
    }

    private suspend fun enrutar(paquete: IncomingPacket) {
        when (paquete.tipo) {
            SshMessage.SSH_MSG_GLOBAL_REQUEST -> {
                rechazarPeticionGlobal(paquete)
            }

            SshMessage.SSH_MSG_REQUEST_SUCCESS, SshMessage.SSH_MSG_REQUEST_FAILURE -> {
                val pendiente =
                    peticionesGlobales.poll() ?: throw SshProtocolException("Respuesta a una petición global no realizada")
                pendiente.complete(paquete.tipo == SshMessage.SSH_MSG_REQUEST_SUCCESS)
            }

            SshMessage.SSH_MSG_CHANNEL_OPEN -> {
                rechazarApertura(paquete)
            }

            in SshMessage.SSH_MSG_CHANNEL_OPEN_CONFIRMATION..SshMessage.SSH_MSG_CHANNEL_FAILURE -> {
                val lector = SshReader(paquete.payload)
                lector.leerByte()
                val id = lector.leerUint32().toInt()
                val canal = canales[id] ?: throw SshProtocolException("Mensaje para un canal inexistente: $id")
                canal.recibirMensaje(paquete)
            }

            else -> {
                transport.enviar(
                    SshWriter().escribirByte(SshMessage.SSH_MSG_UNIMPLEMENTED).escribirUint32(paquete.secuencia).bytes(),
                )
            }
        }
    }

    private suspend fun rechazarPeticionGlobal(paquete: IncomingPacket) {
        val lector = SshReader(paquete.payload)
        lector.leerByte()
        lector.leerTexto()
        if (lector.leerBoolean()) {
            transport.enviar(byteArrayOf(SshMessage.SSH_MSG_REQUEST_FAILURE.toByte()))
        }
    }

    private suspend fun rechazarApertura(paquete: IncomingPacket) {
        val lector = SshReader(paquete.payload)
        lector.leerByte()
        lector.leerTexto()
        val idRemitente = lector.leerUint32().toInt()
        transport.enviar(
            SshWriter()
                .escribirByte(SshMessage.SSH_MSG_CHANNEL_OPEN_FAILURE)
                .escribirUint32(idRemitente)
                .escribirUint32(SshMessage.SSH_OPEN_ADMINISTRATIVELY_PROHIBITED)
                .escribirTexto("TecoTerm no acepta canales abiertos por el servidor")
                .escribirTexto("")
                .bytes(),
        )
    }

    private fun liberar(id: Int) {
        canales.remove(id)
    }
}
