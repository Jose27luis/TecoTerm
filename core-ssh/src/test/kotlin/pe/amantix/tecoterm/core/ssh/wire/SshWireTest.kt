package pe.amantix.tecoterm.core.ssh.wire

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pe.amantix.tecoterm.core.ssh.SshProtocolException
import java.math.BigInteger

class SshWireTest {
    @Test
    fun `los enteros sin signo van y vuelven`() {
        val bytes =
            SshWriter()
                .escribirUint32(0)
                .escribirUint32(0xFFFF_FFFFL.toInt())
                .escribirUint64(Long.MAX_VALUE)
                .escribirByte(0xAB)
                .escribirBoolean(true)
                .escribirBoolean(false)
                .bytes()
        val lector = SshReader(bytes)

        assertEquals(0L, lector.leerUint32())
        assertEquals(0xFFFF_FFFFL, lector.leerUint32())
        assertEquals(Long.MAX_VALUE, lector.leerUint64())
        assertEquals(0xAB, lector.leerByte())
        assertTrue(lector.leerBoolean())
        assertFalse(lector.leerBoolean())
        lector.verificarFin()
    }

    @Test
    fun `los mpint siguen los ejemplos de la RFC 4251`() {
        val casos =
            mapOf(
                BigInteger.ZERO to "00000000",
                BigInteger("9a378f9b2e332a7", 16) to "0000000809a378f9b2e332a7",
                BigInteger("80", 16) to "000000020080",
                BigInteger("-1234", 16) to "00000002edcc",
                BigInteger("-deadbeef", 16) to "00000005ff21524111",
            )

        casos.forEach { (valor, esperado) ->
            val bytes = SshWriter().escribirMpint(valor).bytes()
            assertEquals(esperado, hex(bytes))
            assertEquals(valor, SshReader(bytes).leerMpint())
        }
    }

    @Test
    fun `el mpint sin signo quita ceros y agrega el byte de signo`() {
        assertEquals("000000020080", hex(SshWriter().escribirMpintSinSigno(byteArrayOf(0, 0, 0x80.toByte())).bytes()))
        assertEquals("000000017f", hex(SshWriter().escribirMpintSinSigno(byteArrayOf(0, 0x7F)).bytes()))
        assertEquals("00000000", hex(SshWriter().escribirMpintSinSigno(ByteArray(4)).bytes()))
    }

    @Test
    fun `textos y listas de nombres van y vuelven`() {
        val bytes =
            SshWriter()
                .escribirTexto("conexión")
                .escribirNameList(listOf("aes256-ctr", "aes128-ctr"))
                .escribirNameList(emptyList())
                .bytes()
        val lector = SshReader(bytes)

        assertEquals("conexión", lector.leerTexto())
        assertEquals(listOf("aes256-ctr", "aes128-ctr"), lector.leerNameList())
        assertEquals(emptyList<String>(), lector.leerNameList())
    }

    @Test
    fun `una longitud mayor que los datos se rechaza`() {
        val bytes = SshWriter().escribirUint32(10).escribirBytes(byteArrayOf(1, 2)).bytes()

        assertThrows<SshProtocolException> { SshReader(bytes).leerString() }
    }

    @Test
    fun `los datos sobrantes se rechazan`() {
        val lector = SshReader(byteArrayOf(1, 2))
        lector.leerByte()

        assertThrows<SshProtocolException> { lector.verificarFin() }
    }

    @Test
    fun `el UTF-8 invalido se rechaza`() {
        val bytes = SshWriter().escribirString(byteArrayOf(0xC3.toByte(), 0x28)).bytes()

        assertThrows<SshProtocolException> { SshReader(bytes).leerTexto() }
    }

    @Test
    fun `limpiar borra el contenido escrito`() {
        val escritor = SshWriter().escribirBytes(byteArrayOf(1, 2, 3))
        escritor.limpiar()

        assertArrayEquals(ByteArray(0), escritor.bytes())
    }

    private fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
}
