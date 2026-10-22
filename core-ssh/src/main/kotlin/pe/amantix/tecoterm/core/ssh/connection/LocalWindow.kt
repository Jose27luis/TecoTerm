package pe.amantix.tecoterm.core.ssh.connection

import java.util.concurrent.atomic.AtomicLong

internal class LocalWindow(
    val tamano: Long,
) {
    private val disponible: AtomicLong = AtomicLong(tamano)
    private val consumido: AtomicLong = AtomicLong(0)

    fun recibir(bytes: Int): Boolean {
        while (true) {
            val actual = disponible.get()
            if (bytes > actual) {
                return false
            }
            if (disponible.compareAndSet(actual, actual - bytes)) {
                return true
            }
        }
    }

    fun consumir(bytes: Int): Long? {
        if (consumido.addAndGet(bytes.toLong()) < tamano / 2) {
            return null
        }
        val ajuste = consumido.getAndSet(0)
        disponible.addAndGet(ajuste)
        return ajuste
    }
}
