package pe.amantix.tecoterm.core.ssh.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.amantix.tecoterm.core.ssh.ConnectionClosedException
import pe.amantix.tecoterm.core.ssh.ConnectionLostException
import pe.amantix.tecoterm.core.ssh.SshException
import pe.amantix.tecoterm.core.ssh.connection.ChannelMultiplexer
import pe.amantix.tecoterm.core.ssh.connection.Keepalive
import pe.amantix.tecoterm.core.ssh.connection.PtyRequest
import pe.amantix.tecoterm.core.ssh.connection.SessionChannel
import pe.amantix.tecoterm.core.ssh.transport.Transport
import pe.amantix.tecoterm.core.ssh.wire.SshMessage

public class SshConnection internal constructor(
    internal val transport: Transport,
    private val alcance: CoroutineScope,
    config: SshConfig,
) {
    private val _estado: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Conectada)
    public val estado: StateFlow<ConnectionState> = _estado.asStateFlow()

    private val multiplexer: ChannelMultiplexer = ChannelMultiplexer(transport, ::terminar)

    init {
        multiplexer.iniciar(alcance)
        config.intervaloKeepalive?.let { intervalo ->
            Keepalive(multiplexer, intervalo, config.intentosKeepalive) {
                transport.cerrar(ConnectionLostException("El servidor dejó de responder"))
            }.iniciar(alcance)
        }
    }

    public suspend fun abrirShell(pty: PtyRequest = PtyRequest()): SessionChannel {
        val canal = multiplexer.abrirSesion()
        canal.solicitarPty(pty)
        canal.iniciarShell()
        return canal
    }

    public suspend fun ejecutar(comando: String): SessionChannel {
        val canal = multiplexer.abrirSesion()
        canal.ejecutar(comando)
        return canal
    }

    public suspend fun desconectar() {
        transport.desconectar(SshMessage.SSH_DISCONNECT_BY_APPLICATION, "Desconexión solicitada por el usuario")
    }

    private fun terminar(causa: SshException) {
        _estado.value = ConnectionState.Desconectada(causa.takeUnless { it is ConnectionClosedException })
        alcance.cancel()
    }
}
