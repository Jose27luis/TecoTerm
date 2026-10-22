package pe.amantix.tecoterm.core.ssh.integration

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.time.Duration.Companion.seconds

internal class RekeyTest {
    @Test
    fun `los datos llegan integros a traves de varios re-key en ambas direcciones`(): Unit =
        runBlocking {
            withTimeout(120.seconds) {
                val conexion = TestServer.conectar(config = TestServer.config().copy(limiteBytesRekey = 64L * 1024))
                val descarga =
                    TestServer.ejecutar(conexion, "head -c 8388608 /dev/urandom > /tmp/rekey && sha256sum /tmp/rekey >&2 && cat /tmp/rekey")
                assertEquals(8_388_608, descarga.salida.size)
                assertEquals(descarga.erroresTexto.substringBefore(' '), sha256(descarga.salida))

                val datos = ByteArray(2 * 1024 * 1024).also(SecureRandom()::nextBytes)
                val canal = conexion.ejecutar("sha256sum")
                val subida =
                    coroutineScope {
                        val resultado = async { TestServer.esperar(canal) }
                        canal.escribir(datos)
                        canal.enviarEof()
                        resultado.await()
                    }
                assertEquals(sha256(datos), subida.salidaTexto.substringBefore(' '))

                val intercambios = conexion.transport.intercambiosRealizados
                conexion.desconectar()
                assertTrue(intercambios > 10, "Se esperaban varios re-key y hubo $intercambios")
            }
        }

    private fun sha256(datos: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(datos).joinToString("") { "%02x".format(it) }
}
