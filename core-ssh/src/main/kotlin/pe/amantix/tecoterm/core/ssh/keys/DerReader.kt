package pe.amantix.tecoterm.core.ssh.keys

import pe.amantix.tecoterm.core.ssh.InvalidKeyFileException
import java.math.BigInteger

internal class DerReader(
    private val datos: ByteArray,
) {
    private var posicion: Int = 0

    val tieneMas: Boolean
        get() = posicion < datos.size

    val siguienteEtiqueta: Int
        get() = if (tieneMas) datos[posicion].toInt() and MASCARA_BYTE else SIN_ETIQUETA

    fun leerSecuencia(): DerReader = DerReader(leer(ETIQUETA_SECUENCIA))

    fun leerEntero(): BigInteger = BigInteger(leer(ETIQUETA_ENTERO))

    fun leerOctetos(): ByteArray = leer(ETIQUETA_OCTETOS)

    fun leerBits(): ByteArray {
        val contenido = leer(ETIQUETA_BITS)
        if (contenido.isEmpty() || contenido[0].toInt() != 0) {
            throw InvalidKeyFileException("BIT STRING con bits sobrantes")
        }
        return contenido.copyOfRange(1, contenido.size)
    }

    fun leerOid(): String {
        val contenido = leer(ETIQUETA_OID)
        if (contenido.isEmpty()) {
            throw InvalidKeyFileException("Identificador de objeto vacío")
        }
        val primero = contenido[0].toInt() and MASCARA_BYTE
        val partes = mutableListOf(primero / BASE_PRIMER_ARCO, primero % BASE_PRIMER_ARCO)
        var valor = 0L
        for (indice in 1 until contenido.size) {
            val byte = contenido[indice].toInt() and MASCARA_BYTE
            valor = (valor shl BITS_SIETE) or (byte and MASCARA_SIETE).toLong()
            if (byte and BIT_CONTINUACION == 0) {
                partes += valor.toInt()
                valor = 0
            }
        }
        return partes.joinToString(".")
    }

    fun leerContexto(numero: Int): DerReader = DerReader(leer(ETIQUETA_CONTEXTO or numero))

    fun saltar() {
        leer(siguienteEtiqueta)
    }

    private fun leer(etiquetaEsperada: Int): ByteArray {
        if (siguienteEtiqueta != etiquetaEsperada) {
            throw InvalidKeyFileException("Estructura DER inesperada")
        }
        posicion++
        val longitud = leerLongitud()
        if (longitud > datos.size - posicion) {
            throw InvalidKeyFileException("Estructura DER truncada")
        }
        val contenido = datos.copyOfRange(posicion, posicion + longitud)
        posicion += longitud
        return contenido
    }

    private fun leerLongitud(): Int {
        if (!tieneMas) {
            throw InvalidKeyFileException("Estructura DER truncada")
        }
        val primero = datos[posicion++].toInt() and MASCARA_BYTE
        if (primero and BIT_CONTINUACION == 0) {
            return primero
        }
        val cantidad = primero and MASCARA_SIETE
        if (cantidad == 0 || cantidad > BYTES_LONGITUD_MAXIMA || cantidad > datos.size - posicion) {
            throw InvalidKeyFileException("Longitud DER inválida")
        }
        var longitud = 0
        repeat(cantidad) { longitud = (longitud shl BITS_BYTE) or (datos[posicion++].toInt() and MASCARA_BYTE) }
        return longitud
    }

    companion object {
        const val ETIQUETA_SECUENCIA: Int = 0x30
        const val ETIQUETA_ENTERO: Int = 0x02
        const val ETIQUETA_OCTETOS: Int = 0x04
        const val ETIQUETA_BITS: Int = 0x03
        const val ETIQUETA_OID: Int = 0x06
        const val ETIQUETA_CONTEXTO: Int = 0xA0
        private const val SIN_ETIQUETA: Int = -1
        private const val MASCARA_BYTE: Int = 0xFF
        private const val MASCARA_SIETE: Int = 0x7F
        private const val BIT_CONTINUACION: Int = 0x80
        private const val BITS_SIETE: Int = 7
        private const val BITS_BYTE: Int = 8
        private const val BASE_PRIMER_ARCO: Int = 40
        private const val BYTES_LONGITUD_MAXIMA: Int = 3
    }
}
