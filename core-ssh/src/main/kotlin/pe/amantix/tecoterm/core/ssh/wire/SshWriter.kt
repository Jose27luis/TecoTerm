package pe.amantix.tecoterm.core.ssh.wire

import java.math.BigInteger

internal class SshWriter(
    capacidadInicial: Int = CAPACIDAD_INICIAL,
) {
    private var buffer: ByteArray = ByteArray(capacidadInicial)
    private var longitud: Int = 0

    fun escribirByte(valor: Int): SshWriter {
        asegurar(1)
        buffer[longitud++] = valor.toByte()
        return this
    }

    fun escribirBoolean(valor: Boolean): SshWriter = escribirByte(if (valor) 1 else 0)

    fun escribirUint32(valor: Int): SshWriter {
        asegurar(BYTES_UINT32)
        for (desplazamiento in BITS_ULTIMO_BYTE downTo 0 step BITS_BYTE) {
            buffer[longitud++] = (valor ushr desplazamiento).toByte()
        }
        return this
    }

    fun escribirUint64(valor: Long): SshWriter {
        escribirUint32((valor ushr BITS_UINT32).toInt())
        return escribirUint32(valor.toInt())
    }

    fun escribirBytes(bytes: ByteArray): SshWriter {
        asegurar(bytes.size)
        bytes.copyInto(buffer, longitud)
        longitud += bytes.size
        return this
    }

    fun escribirString(bytes: ByteArray): SshWriter {
        escribirUint32(bytes.size)
        return escribirBytes(bytes)
    }

    fun escribirTexto(texto: String): SshWriter = escribirString(texto.toByteArray(Charsets.UTF_8))

    fun escribirMpint(valor: BigInteger): SshWriter =
        if (valor.signum() == 0) {
            escribirUint32(0)
        } else {
            escribirString(valor.toByteArray())
        }

    fun escribirMpintSinSigno(bytes: ByteArray): SshWriter {
        val inicio = bytes.indexOfFirst { it.toInt() != 0 }
        if (inicio < 0) {
            return escribirUint32(0)
        }
        val necesitaCero = bytes[inicio] < 0
        escribirUint32(bytes.size - inicio + if (necesitaCero) 1 else 0)
        if (necesitaCero) {
            escribirByte(0)
        }
        asegurar(bytes.size - inicio)
        bytes.copyInto(buffer, longitud, inicio)
        longitud += bytes.size - inicio
        return this
    }

    fun escribirNameList(nombres: List<String>): SshWriter = escribirString(nombres.joinToString(",").toByteArray(Charsets.US_ASCII))

    fun bytes(): ByteArray = buffer.copyOf(longitud)

    fun limpiar() {
        buffer.fill(0)
        longitud = 0
    }

    private fun asegurar(adicional: Int) {
        val requerido = longitud + adicional
        if (requerido > buffer.size) {
            val nuevo = buffer.copyOf(maxOf(requerido, buffer.size * 2))
            buffer.fill(0)
            buffer = nuevo
        }
    }

    private companion object {
        const val CAPACIDAD_INICIAL: Int = 256
        const val BYTES_UINT32: Int = 4
        const val BITS_BYTE: Int = 8
        const val BITS_ULTIMO_BYTE: Int = 24
        const val BITS_UINT32: Int = 32
    }
}
