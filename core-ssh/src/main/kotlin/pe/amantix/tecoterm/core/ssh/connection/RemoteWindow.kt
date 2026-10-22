package pe.amantix.tecoterm.core.ssh.connection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import pe.amantix.tecoterm.core.ssh.ConnectionClosedException

internal class RemoteWindow {
    private val ventana: MutableStateFlow<Long> = MutableStateFlow(0)

    fun agregar(bytes: Long) {
        ventana.update { actual -> if (actual == CERRADA) CERRADA else minOf(actual + bytes, MAXIMA) }
    }

    fun cerrar() {
        ventana.value = CERRADA
    }

    suspend fun reservar(maximo: Int): Int {
        while (true) {
            val actual = ventana.first { it != 0L }
            if (actual == CERRADA) {
                throw ConnectionClosedException()
            }
            val reservado = minOf(actual, maximo.toLong())
            if (ventana.compareAndSet(actual, actual - reservado)) {
                return reservado.toInt()
            }
        }
    }

    private companion object {
        const val CERRADA: Long = -1
        const val MAXIMA: Long = 0xFFFF_FFFFL
    }
}
