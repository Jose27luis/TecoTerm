package pe.amantix.tecoterm.core.security

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

internal class AesGcmEnvelopeTest {
    private val clave: SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val sobre = AesGcmEnvelope { clave }
    private val asociados = "credencial-1".toByteArray()

    @Test
    fun `lo cifrado se descifra igual`() {
        val plano = "contenido secreto".toByteArray()

        assertArrayEquals(plano, sobre.descifrar(sobre.cifrar(plano, asociados), asociados))
    }

    @Test
    fun `dos cifrados del mismo contenido son distintos`() {
        val plano = "contenido".toByteArray()

        assertFalse(sobre.cifrar(plano, asociados).contentEquals(sobre.cifrar(plano, asociados)))
    }

    @Test
    fun `un contenido alterado se rechaza`() {
        val cifrado = sobre.cifrar("contenido".toByteArray(), asociados)
        cifrado[cifrado.size - 1] = (cifrado[cifrado.size - 1].toInt() xor 1).toByte()

        assertThrows<CredentialUnavailableException> { sobre.descifrar(cifrado, asociados) }
    }

    @Test
    fun `un contenido movido a otra credencial se rechaza`() {
        val cifrado = sobre.cifrar("contenido".toByteArray(), asociados)

        assertThrows<CredentialUnavailableException> { sobre.descifrar(cifrado, "credencial-2".toByteArray()) }
    }

    @Test
    fun `cifrar y borrar deja el plano en ceros`() {
        val cifrador = CredentialCipher(sobre, "prueba")
        val plano = "clave privada".toByteArray()
        val cifrado = cifrador.cifrarYBorrar(plano, "id")

        assertArrayEquals(ByteArray(plano.size), plano)
        assertArrayEquals("clave privada".toByteArray(), cifrador.descifrar(cifrado, "id"))
    }
}
