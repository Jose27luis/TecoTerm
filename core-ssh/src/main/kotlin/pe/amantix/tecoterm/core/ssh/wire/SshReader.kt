package pe.amantix.tecoterm.core.ssh.wire

import pe.amantix.tecoterm.core.ssh.SshProtocolException
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

internal class SshReader(
    private val datos: ByteArray,
) {
    private var posicion: Int = 0

    val restantes: Int
        get() = datos.size - posicion

    fun leerByte(): Int {
        exigir(1)
        return datos[posicion++].toInt() and MASCARA_BYTE
    }

    fun leerBoolean(): Boolean = leerByte() != 0

    fun leerUint32(): Long {
        exigir(BYTES_UINT32)
        var valor = 0L
        repeat(BYTES_UINT32) {
            valor = (valor shl BITS_BYTE) or (datos[posicion++].toLong() and MASCARA_BYTE.toLong())
        }
        return valor
    }

    private fun leerLongitud(): Int {
        val longitud = leerUint32()
        if (longitud > restantes) {
            throw SshProtocolException("Longitud $longitud mayor que los $restantes bytes disponibles")
        }
        return longitud.toInt()
    }

    fun leerUint64(): Long {
        val alta = leerUint32()
        val baja = leerUint32()
        return (alta shl BITS_UINT32) or baja
    }

    fun leerBytes(cantidad: Int): ByteArray {
        if (cantidad < 0) {
            throw SshProtocolException("Cantidad de bytes negativa")
        }
        exigir(cantidad)
        val resultado = datos.copyOfRange(posicion, posicion + cantidad)
        posicion += cantidad
        return resultado
    }

    fun leerString(): ByteArray = leerBytes(leerLongitud())

    fun leerTexto(): String {
        val decodificador =
            Charsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decodificador.decode(ByteBuffer.wrap(leerString())).toString()
        } catch (error: CharacterCodingException) {
            throw SshProtocolException("Texto UTF-8 inválido", error)
        }
    }

    fun leerMpint(): BigInteger {
        val bytes = leerString()
        return if (bytes.isEmpty()) BigInteger.ZERO else BigInteger(bytes)
    }

    fun leerNameList(): List<String> {
        val texto = leerString()
        if (texto.any { it < ASCII_IMPRIMIBLE_MINIMO || it > ASCII_IMPRIMIBLE_MAXIMO }) {
            throw SshProtocolException("Lista de nombres con caracteres no ASCII")
        }
        val lista = String(texto, Charsets.US_ASCII)
        return if (lista.isEmpty()) emptyList() else lista.split(',')
    }

    fun verificarFin() {
        if (restantes != 0) {
            throw SshProtocolException("Sobran $restantes bytes en el mensaje")
        }
    }

    private fun exigir(cantidad: Int) {
        if (cantidad > restantes) {
            throw SshProtocolException("Mensaje truncado: se esperaban $cantidad bytes y quedan $restantes")
        }
    }

    private companion object {
        const val MASCARA_BYTE: Int = 0xFF
        const val BITS_BYTE: Int = 8
        const val BYTES_UINT32: Int = 4
        const val BITS_UINT32: Int = 32
        const val ASCII_IMPRIMIBLE_MINIMO: Byte = 0x20
        const val ASCII_IMPRIMIBLE_MAXIMO: Byte = 0x7E
    }
}
