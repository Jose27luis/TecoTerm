package pe.amantix.tecoterm.core.ssh.transport

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import pe.amantix.tecoterm.core.ssh.wire.SshWriter
import java.security.MessageDigest

class KeyDerivationTest {
    private val secreto = ByteArray(32) { (it + 1).toByte() }
    private val hash = ByteArray(32) { (it * 3).toByte() }
    private val idSesion = ByteArray(32) { (it * 7).toByte() }

    @Test
    fun `la primera parte es el hash de K, H, la letra y el id de sesion`() {
        val k = SshWriter().escribirMpintSinSigno(secreto).bytes()
        val esperado = sha256(k + hash + byteArrayOf('C'.code.toByte()) + idSesion)

        assertArrayEquals(esperado, KeyDerivation(secreto, hash, idSesion).derivar('C', 32))
    }

    @Test
    fun `las claves largas se extienden con K, H y lo ya derivado`() {
        val k = SshWriter().escribirMpintSinSigno(secreto).bytes()
        val primera = sha256(k + hash + byteArrayOf('E'.code.toByte()) + idSesion)
        val segunda = sha256(k + hash + primera)

        val clave = KeyDerivation(secreto, hash, idSesion).derivar('E', 64)

        assertEquals(64, clave.size)
        assertArrayEquals(primera + segunda, clave)
    }

    @Test
    fun `cada letra produce una clave distinta`() {
        val derivacion = KeyDerivation(secreto, hash, idSesion)

        assertFalse(derivacion.derivar('A', 16).contentEquals(derivacion.derivar('B', 16)))
    }

    private fun sha256(datos: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(datos)
}
