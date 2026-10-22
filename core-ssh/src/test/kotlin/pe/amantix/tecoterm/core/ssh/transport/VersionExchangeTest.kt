package pe.amantix.tecoterm.core.ssh.transport

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pe.amantix.tecoterm.core.ssh.ConnectionLostException
import pe.amantix.tecoterm.core.ssh.SshProtocolException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class VersionExchangeTest {
    @Test
    fun `se ignoran las lineas previas a la version`() {
        val entrada = ByteArrayInputStream("Bienvenido\r\nservidor de pruebas\nSSH-2.0-OpenSSH_10.3\r\n".toByteArray())

        assertEquals("SSH-2.0-OpenSSH_10.3", VersionExchange.leer(entrada))
    }

    @Test
    fun `SSH 1 99 se acepta como compatible`() {
        assertEquals("SSH-1.99-Antiguo", VersionExchange.leer(ByteArrayInputStream("SSH-1.99-Antiguo\r\n".toByteArray())))
    }

    @Test
    fun `SSH 1 se rechaza`() {
        assertThrows<SshProtocolException> { VersionExchange.leer(ByteArrayInputStream("SSH-1.5-Viejo\r\n".toByteArray())) }
    }

    @Test
    fun `una linea demasiado larga se rechaza`() {
        assertThrows<SshProtocolException> { VersionExchange.leer(ByteArrayInputStream("x".repeat(300).toByteArray())) }
    }

    @Test
    fun `el cierre antes de la version es una conexion perdida`() {
        assertThrows<ConnectionLostException> { VersionExchange.leer(ByteArrayInputStream("Hola".toByteArray())) }
    }

    @Test
    fun `la version del cliente termina en CRLF`() {
        val salida = ByteArrayOutputStream()
        VersionExchange.enviar(salida)

        assertEquals("${VersionExchange.VERSION_CLIENTE}\r\n", salida.toString(Charsets.US_ASCII))
    }
}
