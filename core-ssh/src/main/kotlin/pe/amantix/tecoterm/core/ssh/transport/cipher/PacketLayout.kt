package pe.amantix.tecoterm.core.ssh.transport.cipher

import pe.amantix.tecoterm.core.ssh.SshProtocolException
import java.security.SecureRandom

internal object PacketLayout {
    const val BYTES_LONGITUD: Int = 4
    const val TAMANO_MAXIMO_PAQUETE: Int = 256 * 1024
    private const val RELLENO_MINIMO: Int = 4
    private const val MASCARA_BYTE: Int = 0xFF
    private const val BITS_BYTE: Int = 8

    fun construir(
        payload: ByteArray,
        bloque: Int,
        alineacionIncluyeLongitud: Boolean,
        aleatorio: SecureRandom,
    ): ByteArray {
        val alineado = 1 + payload.size + if (alineacionIncluyeLongitud) BYTES_LONGITUD else 0
        var relleno = bloque - alineado % bloque
        if (relleno < RELLENO_MINIMO) {
            relleno += bloque
        }
        val longitudPaquete = 1 + payload.size + relleno
        val paquete = ByteArray(BYTES_LONGITUD + longitudPaquete)
        escribirLongitud(paquete, longitudPaquete)
        paquete[BYTES_LONGITUD] = relleno.toByte()
        payload.copyInto(paquete, BYTES_LONGITUD + 1)
        val bytesRelleno = ByteArray(relleno)
        aleatorio.nextBytes(bytesRelleno)
        bytesRelleno.copyInto(paquete, BYTES_LONGITUD + 1 + payload.size)
        return paquete
    }

    fun leerLongitud(
        bytes: ByteArray,
        bloque: Int,
        alineacionIncluyeLongitud: Boolean,
    ): Int {
        var longitud = 0L
        for (indice in 0 until BYTES_LONGITUD) {
            longitud = (longitud shl BITS_BYTE) or (bytes[indice].toLong() and MASCARA_BYTE.toLong())
        }
        val alineado = longitud + if (alineacionIncluyeLongitud) BYTES_LONGITUD else 0
        if (longitud < RELLENO_MINIMO + 1 || longitud > TAMANO_MAXIMO_PAQUETE || alineado % bloque != 0L) {
            throw SshProtocolException("Longitud de paquete inválida: $longitud")
        }
        return longitud.toInt()
    }

    fun extraerPayload(
        cuerpo: ByteArray,
        desplazamiento: Int,
    ): ByteArray {
        val longitud = cuerpo.size - desplazamiento
        val relleno = cuerpo[desplazamiento].toInt() and MASCARA_BYTE
        if (relleno < RELLENO_MINIMO || relleno >= longitud) {
            throw SshProtocolException("Relleno de paquete inválido")
        }
        return cuerpo.copyOfRange(desplazamiento + 1, cuerpo.size - relleno)
    }

    fun escribirLongitud(
        destino: ByteArray,
        valor: Int,
    ) {
        for (indice in 0 until BYTES_LONGITUD) {
            destino[indice] = (valor ushr (BITS_BYTE * (BYTES_LONGITUD - 1 - indice))).toByte()
        }
    }

    fun bytesSecuencia(secuencia: Int): ByteArray = ByteArray(BYTES_LONGITUD).also { escribirLongitud(it, secuencia) }
}
