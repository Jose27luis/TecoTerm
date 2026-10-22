package pe.amantix.tecoterm.core.ssh.transport

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pe.amantix.tecoterm.core.ssh.SshProtocolException
import pe.amantix.tecoterm.core.ssh.crypto.PlatformAlgorithms

class KexInitTest {
    @Test
    fun `el KEXINIT propio se lee igual que se escribio`() {
        val listas = AlgorithmNegotiator(PlatformAlgorithms::disponibles).propuesta(primerIntercambio = true)
        val creado = KexInit.crear(ByteArray(KexInit.LONGITUD_COOKIE) { it.toByte() }, listas)

        val leido = KexInit.leer(creado.payload)

        assertEquals(listas, leido.listas)
        assertFalse(leido.primerPaqueteSigue)
        assertArrayEquals(creado.payload, leido.payload)
    }

    @Test
    fun `un mensaje que no es KEXINIT se rechaza`() {
        assertThrows<SshProtocolException> { KexInit.leer(byteArrayOf(21)) }
    }
}
