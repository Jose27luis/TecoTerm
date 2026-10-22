package pe.amantix.tecoterm.core.ssh.crypto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pe.amantix.tecoterm.core.ssh.SshProtocolException
import pe.amantix.tecoterm.core.ssh.wire.SshWriter
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.util.Base64

class HostKeyTest {
    @Test
    fun `la huella coincide con la de ssh-keygen`() {
        val blob = Base64.getDecoder().decode("AAAAC3NzaC1lZDI1NTE5AAAAIFRZPOpZ201YFxi3OfSwEkA5cS4eMgkdHUkwRR6hspzD")

        assertEquals("SHA256:G/JohHOK+ynuOuXTXhRphEfttqhB3I/JYxNctmROSAU", HostKey.desdeBlob(blob).huellaSha256())
    }

    @Test
    fun `cada tipo de clave firma y verifica con todos sus algoritmos`() {
        listOf(parEd25519(), parRsa(), parEcdsa()).forEach { par ->
            val cliente = ClientKey.desde(par)
            val servidor = HostKey.desdeBlob(cliente.blobPublico)
            assertEquals(cliente.tipo, servidor.tipo)
            cliente.algoritmosFirma.forEach { algoritmo ->
                val datos = "intercambio $algoritmo".toByteArray()
                val firma = cliente.firmar(algoritmo, datos)
                assertTrue(servidor.verificar(algoritmo, datos, firma), algoritmo)
                assertFalse(servidor.verificar(algoritmo, datos + byteArrayOf(1), firma), algoritmo)
            }
        }
    }

    @Test
    fun `una firma con otro algoritmo no se acepta`() {
        val cliente = ClientKey.desde(parRsa())
        val servidor = HostKey.desdeBlob(cliente.blobPublico)
        val datos = "datos".toByteArray()

        assertFalse(servidor.verificar("rsa-sha2-256", datos, cliente.firmar("rsa-sha2-512", datos)))
        assertFalse(servidor.verificar("ssh-ed25519", datos, cliente.firmar("rsa-sha2-512", datos)))
    }

    @Test
    fun `dos blobs iguales son la misma clave`() {
        val cliente = ClientKey.desde(parEd25519())

        assertEquals(HostKey.desdeBlob(cliente.blobPublico), HostKey.desdeBlob(cliente.blobPublico))
    }

    @Test
    fun `un tipo desconocido se rechaza`() {
        val blob = SshWriter().escribirTexto("ssh-dss").bytes()

        assertThrows<SshProtocolException> { HostKey.desdeBlob(blob) }
    }

    @Test
    fun `RSA menor a 2048 bits se rechaza`() {
        val par = KeyPairGenerator.getInstance("RSA").apply { initialize(1024) }.generateKeyPair()

        assertThrows<IllegalArgumentException> { ClientKey.desde(par) }
    }

    private fun parEd25519(): KeyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair()

    private fun parRsa(): KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    private fun parEcdsa(): KeyPair =
        KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
}
