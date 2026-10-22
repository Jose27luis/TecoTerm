package pe.amantix.tecoterm.core.ssh.transport

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pe.amantix.tecoterm.core.ssh.SshProtocolException
import pe.amantix.tecoterm.core.ssh.crypto.PlatformAlgorithms
import pe.amantix.tecoterm.core.ssh.crypto.SshAlgorithm

class AlgorithmNegotiatorTest {
    private val negociador = AlgorithmNegotiator(PlatformAlgorithms::disponibles)

    @Test
    fun `gana el primer algoritmo del cliente que tenga el servidor`() {
        val cliente = negociador.propuesta(primerIntercambio = true)
        val servidor =
            servidor(
                intercambio = listOf("ecdh-sha2-nistp256", "curve25519-sha256"),
                cifrado = listOf("aes128-ctr", "aes256-ctr"),
                mac = listOf("hmac-sha2-512", "hmac-sha2-256-etm@openssh.com"),
            )

        val elegidos = negociador.negociar(cliente, servidor, primerIntercambio = true)

        assertEquals(SshAlgorithm.CURVE25519_SHA256, elegidos.intercambio)
        assertEquals(SshAlgorithm.AES256_CTR, elegidos.clienteServidor.cifrado)
        assertEquals(SshAlgorithm.HMAC_SHA2_256_ETM, elegidos.clienteServidor.mac)
    }

    @Test
    fun `con AES-GCM no se negocia MAC`() {
        val elegidos =
            negociador.negociar(
                negociador.propuesta(primerIntercambio = true),
                servidor(cifrado = listOf("aes128-gcm@openssh.com"), mac = emptyList()),
                primerIntercambio = true,
            )

        assertEquals(SshAlgorithm.AES128_GCM, elegidos.servidorCliente.cifrado)
        assertNull(elegidos.servidorCliente.mac)
    }

    @Test
    fun `el kex estricto solo cuenta en el primer intercambio`() {
        val servidor = servidor(intercambio = listOf("curve25519-sha256", AlgorithmNegotiator.KEX_ESTRICTO_SERVIDOR))

        assertTrue(negociador.negociar(negociador.propuesta(true), servidor, primerIntercambio = true).kexEstricto)
        assertFalse(negociador.negociar(negociador.propuesta(false), servidor, primerIntercambio = false).kexEstricto)
    }

    @Test
    fun `los marcadores de extension solo van en el primer KEXINIT`() {
        assertTrue(AlgorithmNegotiator.EXT_INFO_CLIENTE in negociador.propuesta(primerIntercambio = true).intercambio)
        assertFalse(AlgorithmNegotiator.EXT_INFO_CLIENTE in negociador.propuesta(primerIntercambio = false).intercambio)
    }

    @Test
    fun `el tipo de clave conocido del servidor va primero`() {
        val conRsa = AlgorithmNegotiator(PlatformAlgorithms::disponibles, tipoClaveHostConocida = "ssh-rsa")
        val conEcdsa = AlgorithmNegotiator(PlatformAlgorithms::disponibles, tipoClaveHostConocida = "ecdsa-sha2-nistp256")

        assertEquals(listOf("rsa-sha2-512", "rsa-sha2-256"), conRsa.propuesta(true).claveHost.take(2))
        assertEquals("ecdsa-sha2-nistp256", conEcdsa.propuesta(true).claveHost.first())
        assertEquals("ssh-ed25519", negociador.propuesta(true).claveHost.first())
    }

    @Test
    fun `sin algoritmo comun la negociacion falla`() {
        assertThrows<SshProtocolException> {
            negociador.negociar(negociador.propuesta(true), servidor(cifrado = listOf("3des-cbc")), primerIntercambio = true)
        }
    }

    @Test
    fun `sin compresion none la negociacion falla`() {
        assertThrows<SshProtocolException> {
            negociador.negociar(negociador.propuesta(true), servidor(compresion = listOf("zlib")), primerIntercambio = true)
        }
    }

    private fun servidor(
        intercambio: List<String> = listOf("curve25519-sha256"),
        cifrado: List<String> = listOf("aes256-gcm@openssh.com"),
        mac: List<String> = listOf("hmac-sha2-256"),
        compresion: List<String> = listOf("none"),
    ): KexLists =
        KexLists(
            intercambio = intercambio,
            claveHost = listOf("ssh-ed25519"),
            cifradoClienteServidor = cifrado,
            cifradoServidorCliente = cifrado,
            macClienteServidor = mac,
            macServidorCliente = mac,
            compresionClienteServidor = compresion,
            compresionServidorCliente = compresion,
        )
}
