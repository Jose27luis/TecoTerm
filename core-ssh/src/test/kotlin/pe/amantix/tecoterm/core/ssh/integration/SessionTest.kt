package pe.amantix.tecoterm.core.ssh.integration

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pe.amantix.tecoterm.core.ssh.client.SshConnection
import pe.amantix.tecoterm.core.ssh.connection.ExitStatus
import pe.amantix.tecoterm.core.ssh.connection.PtyRequest
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.time.Duration.Companion.seconds

internal class SessionTest {
    @Test
    fun `el shell con PTY ejecuta comandos y entrega el codigo de salida`() =
        conConexion { conexion ->
            val shell = conexion.abrirShell(PtyRequest(columnas = 120, filas = 30))
            shell.escribir("whoami\nexit 0\n".toByteArray())

            val resultado = TestServer.esperar(shell)

            assertTrue(resultado.salidaTexto.contains(TestServer.usuario), resultado.salidaTexto)
            assertEquals(ExitStatus.Codigo(0), resultado.estado)
        }

    @Test
    fun `el cambio de tamano llega al PTY`() =
        conConexion { conexion ->
            val shell = conexion.abrirShell(PtyRequest(columnas = 80, filas = 24))
            shell.cambiarTamano(columnas = 132, filas = 43)
            shell.escribir("stty size\nexit\n".toByteArray())

            assertTrue(TestServer.esperar(shell).salidaTexto.contains("43 132"))
        }

    @Test
    fun `exec separa la salida estandar de la de error`() =
        conConexion { conexion ->
            val resultado = TestServer.ejecutar(conexion, "echo salida; echo error >&2; exit 3")

            assertEquals("salida\n", resultado.salidaTexto)
            assertEquals("error\n", resultado.erroresTexto)
            assertEquals(ExitStatus.Codigo(3), resultado.estado)
        }

    @Test
    fun `una senal termina el proceso remoto`() =
        conConexion { conexion ->
            val canal = conexion.ejecutar("exec sleep 30")
            launch { canal.enviarSenal("KILL") }

            assertEquals(ExitStatus.Senal("KILL", ""), TestServer.esperar(canal).estado)
        }

    @Test
    fun `varios canales funcionan a la vez`() =
        conConexion { conexion ->
            val resultados =
                coroutineScope {
                    (1..5).map { numero -> async { TestServer.ejecutar(conexion, "sleep 0.2; echo $numero").salidaTexto } }.awaitAll()
                }

            assertEquals((1..5).map { "$it\n" }, resultados)
        }

    @Test
    fun `los datos enviados respetan la ventana del servidor`() =
        conConexion { conexion ->
            val datos = ByteArray(4 * 1024 * 1024).also(SecureRandom()::nextBytes)
            val canal = conexion.ejecutar("sha256sum")
            coroutineScope {
                val resultado = async { TestServer.esperar(canal) }
                canal.escribir(datos)
                canal.enviarEof()
                val esperado = MessageDigest.getInstance("SHA-256").digest(datos).joinToString("") { "%02x".format(it) }
                assertEquals(esperado, resultado.await().salidaTexto.substringBefore(' '))
            }
        }

    @Test
    fun `la salida grande llega completa`() =
        conConexion { conexion ->
            val resultado =
                TestServer.ejecutar(conexion, "head -c 3000000 /dev/urandom > /tmp/grande && sha256sum /tmp/grande >&2 && cat /tmp/grande")
            val esperado = resultado.erroresTexto.substringBefore(' ')

            assertEquals(3_000_000, resultado.salida.size)
            assertEquals(esperado, MessageDigest.getInstance("SHA-256").digest(resultado.salida).joinToString("") { "%02x".format(it) })
        }

    private fun conConexion(prueba: suspend CoroutineScope.(SshConnection) -> Unit): Unit =
        runBlocking {
            withTimeout(60.seconds) {
                val conexion = TestServer.conectar()
                try {
                    prueba(conexion)
                } finally {
                    conexion.desconectar()
                }
            }
        }
}
