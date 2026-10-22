package pe.amantix.tecoterm.core.ssh.transport.cipher

import java.io.DataInputStream

internal interface PacketCipher {
    fun empaquetar(
        payload: ByteArray,
        secuencia: Int,
    ): ByteArray

    fun leer(
        entrada: DataInputStream,
        secuencia: Int,
    ): ByteArray
}
