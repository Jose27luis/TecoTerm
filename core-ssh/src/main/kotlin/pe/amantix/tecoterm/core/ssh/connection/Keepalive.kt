package pe.amantix.tecoterm.core.ssh.connection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration

internal class Keepalive(
    private val multiplexer: ChannelMultiplexer,
    private val intervalo: Duration,
    private val intentos: Int,
    private val alPerderse: () -> Unit,
) {
    fun iniciar(alcance: CoroutineScope): Job =
        alcance.launch {
            var fallos = 0
            while (isActive) {
                delay(intervalo)
                val respondio = withTimeoutOrNull(intervalo) { multiplexer.peticionGlobal(PETICION) } != null
                fallos = if (respondio) 0 else fallos + 1
                if (fallos >= intentos) {
                    alPerderse()
                    return@launch
                }
            }
        }

    private companion object {
        const val PETICION: String = "keepalive@openssh.com"
    }
}
