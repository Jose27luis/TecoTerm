package pe.amantix.tecoterm.core.ssh.keys

import java.math.BigInteger

internal object PiDigits {
    private const val BITS_PALABRA: Int = 32
    private const val BITS_GUARDA: Int = 64
    private const val MASCARA_PALABRA: Long = 0xFFFF_FFFFL
    private const val MULTIPLO_ARCOTANGENTE_CINCO: Long = 16
    private const val MULTIPLO_ARCOTANGENTE_239: Long = 4
    private const val DENOMINADOR_CINCO: Long = 5
    private const val DENOMINADOR_239: Long = 239
    private const val PARTE_ENTERA: Long = 3

    fun palabrasFraccionarias(cantidad: Int): IntArray {
        val bits = cantidad * BITS_PALABRA
        val escala = BigInteger.ONE.shiftLeft(bits + BITS_GUARDA)
        val pi =
            arcotangenteInversa(DENOMINADOR_CINCO, escala)
                .multiply(BigInteger.valueOf(MULTIPLO_ARCOTANGENTE_CINCO))
                .subtract(arcotangenteInversa(DENOMINADOR_239, escala).multiply(BigInteger.valueOf(MULTIPLO_ARCOTANGENTE_239)))
                .shiftRight(BITS_GUARDA)
        val fraccion = pi.subtract(BigInteger.valueOf(PARTE_ENTERA).shiftLeft(bits))
        return IntArray(cantidad) { indice ->
            fraccion
                .shiftRight(bits - BITS_PALABRA * (indice + 1))
                .toLong()
                .and(MASCARA_PALABRA)
                .toInt()
        }
    }

    private fun arcotangenteInversa(
        denominador: Long,
        escala: BigInteger,
    ): BigInteger {
        val cuadrado = BigInteger.valueOf(denominador * denominador)
        var potencia = escala.divide(BigInteger.valueOf(denominador))
        var suma = potencia
        var divisor = 1L
        var restar = true
        while (potencia.signum() != 0) {
            potencia = potencia.divide(cuadrado)
            divisor += 2
            val termino = potencia.divide(BigInteger.valueOf(divisor))
            suma = if (restar) suma.subtract(termino) else suma.add(termino)
            restar = !restar
        }
        return suma
    }
}
