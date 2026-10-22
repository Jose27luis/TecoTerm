package pe.amantix.tecoterm.core.ssh.keys

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class BlowfishTest {
    @Test
    fun `las constantes iniciales son los digitos hexadecimales de pi`() {
        val palabras = Blowfish.palabrasIniciales()

        assertEquals(0x243F6A88, palabras[0])
        assertEquals(0x85A308D3.toInt(), palabras[1])
        assertEquals(0x8979FB1B.toInt(), palabras[17])
        assertEquals(0xD1310BA6.toInt(), palabras[18])
    }

    @Test
    fun `cifra igual que los vectores de Eric Young`() {
        val vectores =
            listOf(
                Triple("0000000000000000", "0000000000000000", "4ef997456198dd78"),
                Triple("ffffffffffffffff", "ffffffffffffffff", "51866fd5b85ecb8a"),
                Triple("3000000000000000", "1000000000000001", "7d856f9a613063f2"),
                Triple("1111111111111111", "1111111111111111", "2466dd878b963c9d"),
            )

        vectores.forEach { (clave, plano, esperado) ->
            val cifrador = Blowfish.inicial()
            cifrador.expandirSinDatos(bytes(clave))
            val bloque = bytes(plano)
            val palabras = intArrayOf(palabra(bloque, 0), palabra(bloque, 4))
            cifrador.cifrarBloques(palabras)
            assertEquals(esperado, "%08x%08x".format(palabras[0], palabras[1]), clave)
        }
    }

    private fun bytes(hexadecimal: String): ByteArray = hexadecimal.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private fun palabra(
        bytes: ByteArray,
        desde: Int,
    ): Int = (0 until 4).fold(0) { acumulado, indice -> (acumulado shl 8) or (bytes[desde + indice].toInt() and 0xFF) }
}
