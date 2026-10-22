package pe.amantix.tecoterm.core.ssh.integration

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pe.amantix.tecoterm.core.ssh.ConnectionClosedException
import pe.amantix.tecoterm.core.ssh.ConnectionLostException
import pe.amantix.tecoterm.core.ssh.ConnectionRefusedException
import pe.amantix.tecoterm.core.ssh.ConnectionTimeoutException
import pe.amantix.tecoterm.core.ssh.HostKeyRejectedException
import pe.amantix.tecoterm.core.ssh.client.ConnectionState
import pe.amantix.tecoterm.core.ssh.transport.HostKeyVerifier
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.time.Duration.Companion.seconds

internal class FailureTest {
    @Test
    fun `la clave del servidor rechazada corta la conexion`(): Unit =
        runBlocking {
            val error =
                assertThrows<HostKeyRejectedException> {
                    runBlocking { TestServer.conectar(verificador = HostKeyVerifier { _, _, _ -> false }) }
                }
            assertTrue(TestServer.clavesHost.values.any { it.huellaSha256() == error.huella })
        }

    @Test
    fun `un puerto cerrado es una conexion rechazada`() {
        assertThrows<ConnectionRefusedException> {
            runBlocking { TestServer.conectar(config = TestServer.config().copy(puerto = 2)) }
        }
    }

    @Test
    fun `un servidor mudo agota el tiempo de intercambio`() {
        ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { mudo ->
            val config =
                TestServer.config().copy(host = mudo.inetAddress.hostAddress, puerto = mudo.localPort, tiempoIntercambio = 2.seconds)
            assertThrows<ConnectionTimeoutException> {
                runBlocking { withTimeout(20.seconds) { TestServer.conectar(config = config) } }
            }
        }
    }

    @Test
    fun `matar la sesion en el servidor deja la conexion perdida`(): Unit =
        runBlocking {
            withTimeout(30.seconds) {
                val conexion = TestServer.conectar()
                conexion.ejecutar("kill -KILL \$PPID")
                val estado = conexion.estado.first { it is ConnectionState.Desconectada }
                val causa = (estado as? ConnectionState.Desconectada)?.causa
                assertTrue(causa is ConnectionLostException, "Causa inesperada: $causa")
            }
        }

    @Test
    fun `desconectar deja el estado desconectado sin causa`() {
        runBlocking {
            withTimeout(30.seconds) {
                val conexion = TestServer.conectar()
                conexion.desconectar()
                assertEquals(ConnectionState.Desconectada(null), conexion.estado.first { it is ConnectionState.Desconectada })
                assertThrows<ConnectionClosedException> { runBlocking { conexion.ejecutar("true") } }
            }
        }
    }
}
