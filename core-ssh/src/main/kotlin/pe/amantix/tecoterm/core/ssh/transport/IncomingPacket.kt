package pe.amantix.tecoterm.core.ssh.transport

import pe.amantix.tecoterm.core.ssh.SshProtocolException

internal class IncomingPacket(
    val secuencia: Int,
    val payload: ByteArray,
) {
    val tipo: Int =
        if (payload.isEmpty()) {
            throw SshProtocolException("Paquete sin tipo de mensaje")
        } else {
            payload[0].toInt() and MASCARA_BYTE
        }

    private companion object {
        const val MASCARA_BYTE: Int = 0xFF
    }
}
