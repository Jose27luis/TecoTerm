package pe.amantix.tecoterm.core.ssh.integration

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pe.amantix.tecoterm.core.ssh.AuthenticationFailedException
import pe.amantix.tecoterm.core.ssh.auth.Credential
import pe.amantix.tecoterm.core.ssh.crypto.ClientKey
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import kotlin.time.Duration.Companion.seconds

internal class AuthenticationTest {
    @Test
    fun `la contrasena correcta autentica`() =
        conTiempo {
            quienSoy(listOf(TestServer.contrasena()))
        }

    @Test
    fun `la contrasena incorrecta se rechaza`() =
        conTiempo {
            val error =
                assertThrows<AuthenticationFailedException> {
                    runBlocking { TestServer.conectar(credenciales = listOf(Credential.Password("incorrecta".toCharArray()))) }
                }
            assertTrue("password" in error.metodosRestantes)
        }

    @Test
    fun `una clave Ed25519 registrada autentica`() =
        conTiempo {
            probarClave(KeyPairGenerator.getInstance("Ed25519").generateKeyPair())
        }

    @Test
    fun `una clave RSA registrada autentica con SHA-2`() =
        conTiempo {
            probarClave(KeyPairGenerator.getInstance("RSA").apply { initialize(3072) }.generateKeyPair())
        }

    @Test
    fun `una clave ECDSA P-256 registrada autentica`() =
        conTiempo {
            probarClave(KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair())
        }

    @Test
    fun `una clave no registrada se rechaza`() =
        conTiempo {
            val clave = ClientKey.desde(KeyPairGenerator.getInstance("Ed25519").generateKeyPair())

            assertThrows<AuthenticationFailedException> {
                runBlocking { TestServer.conectar(credenciales = listOf(Credential.PublicKey(clave))) }
            }
        }

    @Test
    fun `si la clave falla se prueba la siguiente credencial`() =
        conTiempo {
            val clave = ClientKey.desde(KeyPairGenerator.getInstance("Ed25519").generateKeyPair())

            quienSoy(listOf(Credential.PublicKey(clave), TestServer.contrasena()))
        }

    @Test
    fun `keyboard-interactive autentica respondiendo al servidor`() =
        conTiempo {
            val conexion =
                TestServer.conectar(credenciales = listOf(Credential.KeyboardInteractive), prompter = TestServer.respondeContrasena)
            assertEquals("${TestServer.usuario}\n", TestServer.ejecutar(conexion, "whoami").salidaTexto)
            conexion.desconectar()
        }

    private suspend fun probarClave(par: KeyPair) {
        val clave = ClientKey.desde(par)
        TestServer.registrarClave(clave)
        quienSoy(listOf(Credential.PublicKey(clave)))
    }

    private suspend fun quienSoy(credenciales: List<Credential>) {
        val conexion = TestServer.conectar(credenciales = credenciales)
        assertEquals("${TestServer.usuario}\n", TestServer.ejecutar(conexion, "whoami").salidaTexto)
        conexion.desconectar()
    }

    private fun conTiempo(prueba: suspend () -> Unit): Unit = runBlocking { withTimeout(60.seconds) { prueba() } }
}
