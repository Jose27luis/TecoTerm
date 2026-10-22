package pe.amantix.tecoterm.core.ssh.crypto

import pe.amantix.tecoterm.core.ssh.SshProtocolException
import java.math.BigInteger

internal object EcdsaSignatureCodec {
    private const val ETIQUETA_SECUENCIA: Int = 0x30
    private const val ETIQUETA_ENTERO: Int = 0x02
    private const val LONGITUD_CORTA_MAXIMA: Int = 0x7F
    private const val MASCARA_BYTE: Int = 0xFF

    fun derDesdeRs(
        r: BigInteger,
        s: BigInteger,
    ): ByteArray {
        val enteros = entero(r) + entero(s)
        exigirLongitudCorta(enteros.size)
        return byteArrayOf(ETIQUETA_SECUENCIA.toByte(), enteros.size.toByte()) + enteros
    }

    fun rsDesdeDer(der: ByteArray): Pair<BigInteger, BigInteger> {
        var posicion = 0

        fun leerEncabezado(etiqueta: Int): Int {
            if (posicion + 2 > der.size || (der[posicion].toInt() and MASCARA_BYTE) != etiqueta) {
                throw SshProtocolException("Firma DER inválida")
            }
            val longitud = der[posicion + 1].toInt() and MASCARA_BYTE
            if (longitud > LONGITUD_CORTA_MAXIMA || posicion + 2 + longitud > der.size) {
                throw SshProtocolException("Firma DER inválida")
            }
            posicion += 2
            return longitud
        }

        fun leerEntero(): BigInteger {
            val longitud = leerEncabezado(ETIQUETA_ENTERO)
            val valor = BigInteger(der.copyOfRange(posicion, posicion + longitud))
            posicion += longitud
            return valor
        }

        val longitudSecuencia = leerEncabezado(ETIQUETA_SECUENCIA)
        if (posicion + longitudSecuencia != der.size) {
            throw SshProtocolException("Firma DER inválida")
        }
        return leerEntero() to leerEntero()
    }

    private fun entero(valor: BigInteger): ByteArray {
        val bytes = valor.toByteArray()
        exigirLongitudCorta(bytes.size)
        return byteArrayOf(ETIQUETA_ENTERO.toByte(), bytes.size.toByte()) + bytes
    }

    private fun exigirLongitudCorta(longitud: Int) {
        if (longitud > LONGITUD_CORTA_MAXIMA) {
            throw SshProtocolException("Firma ECDSA demasiado larga para P-256")
        }
    }
}
