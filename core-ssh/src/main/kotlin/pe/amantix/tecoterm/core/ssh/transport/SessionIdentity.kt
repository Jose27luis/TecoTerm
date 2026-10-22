package pe.amantix.tecoterm.core.ssh.transport

import pe.amantix.tecoterm.core.ssh.crypto.HostKey

internal class SessionIdentity(
    val idSesion: ByteArray,
    val claveHost: HostKey,
    val kexEstricto: Boolean,
)
