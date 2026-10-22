package pe.amantix.tecoterm.core.ssh.connection

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import pe.amantix.tecoterm.core.ssh.SshException

public class ChannelReader internal constructor(
    private val alConsumir: suspend (Int) -> Unit,
) {
    private val datos: Channel<ByteArray> = Channel(Channel.UNLIMITED)

    public suspend fun leer(): ByteArray? {
        val bloque =
            try {
                datos.receive()
            } catch (_: ClosedReceiveChannelException) {
                return null
            }
        alConsumir(bloque.size)
        return bloque
    }

    internal fun entregar(bloque: ByteArray) {
        datos.trySend(bloque)
    }

    internal fun finalizar(causa: SshException?) {
        datos.close(causa)
    }
}
