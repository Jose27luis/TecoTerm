package pe.amantix.tecoterm.core.ssh.keys

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pe.amantix.tecoterm.core.ssh.InvalidKeyFileException
import pe.amantix.tecoterm.core.ssh.crypto.ClientKey
import pe.amantix.tecoterm.core.ssh.crypto.HostKey
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.util.Base64

internal class ClientKeyStorageTest {
    @Test
    fun `una clave se reconstruye desde su blob publico y su PKCS 8`() {
        val pares =
            listOf(
                KeyPairGenerator.getInstance("Ed25519").generateKeyPair(),
                KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair(),
                KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair(),
            )

        pares.forEach { par ->
            val original = ClientKey.desde(par)
            val reconstruida = ClientKey.desdeAlmacen(original.blobPublico, original.pkcs8())
            val algoritmo = reconstruida.algoritmosFirma.first()
            val datos = "prueba".toByteArray()

            assertArrayEquals(original.blobPublico, reconstruida.blobPublico)
            assertTrue(HostKey.desdeBlob(original.blobPublico).verificar(algoritmo, datos, reconstruida.firmar(algoritmo, datos)))
        }
    }

    @Test
    fun `la linea publica sigue el formato de authorized_keys`() {
        val clave = ClientKey.desde(KeyPairGenerator.getInstance("Ed25519").generateKeyPair())
        val partes = clave.lineaPublica("teco@telefono").split(' ')

        assertEquals("ssh-ed25519", partes[0])
        assertArrayEquals(clave.blobPublico, Base64.getDecoder().decode(partes[1]))
        assertEquals("teco@telefono", partes[2])
    }

    @Test
    fun `el generador crea claves Ed25519 en la JVM`() {
        assertEquals("ssh-ed25519", ClientKeyGenerator.generar().tipo)
    }

    @Test
    fun `un texto sin clave se rechaza`() {
        assertThrows<InvalidKeyFileException> { PrivateKeyImporter.importar("no es una clave", null) }
    }
}
