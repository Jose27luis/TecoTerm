package pe.amantix.tecoterm.core.ssh.transport.cipher

import java.io.DataInputStream
import java.security.SecureRandom

internal class PlainPacketCipher(
    private val aleatorio: SecureRandom,
) : PacketCipher {
    override fun empaquetar(
        payload: ByteArray,
        secuencia: Int,
    ): ByteArray = PacketLayout.construir(payload, BLOQUE, alineacionIncluyeLongitud = true, aleatorio = aleatorio)

    override fun leer(
        entrada: DataInputStream,
        secuencia: Int,
    ): ByteArray {
        val cabecera = ByteArray(PacketLayout.BYTES_LONGITUD)
        entrada.readFully(cabecera)
        val longitud = PacketLayout.leerLongitud(cabecera, BLOQUE, alineacionIncluyeLongitud = true)
        val cuerpo = ByteArray(longitud)
        entrada.readFully(cuerpo)
        return PacketLayout.extraerPayload(cuerpo, 0)
    }

    private companion object {
        const val BLOQUE: Int = 8
    }
}
