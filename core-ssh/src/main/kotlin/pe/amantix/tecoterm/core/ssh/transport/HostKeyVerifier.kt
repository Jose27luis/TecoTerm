package pe.amantix.tecoterm.core.ssh.transport

import pe.amantix.tecoterm.core.ssh.crypto.HostKey

public fun interface HostKeyVerifier {
    public suspend fun verificar(
        host: String,
        puerto: Int,
        clave: HostKey,
    ): Boolean
}
