package pe.amantix.tecoterm.core.ssh.client

import pe.amantix.tecoterm.core.ssh.crypto.SshAlgorithm
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

public data class SshConfig(
    public val host: String,
    public val usuario: String,
    public val puerto: Int = PUERTO_SSH,
    public val tiempoConexion: Duration = 15.seconds,
    public val tiempoIntercambio: Duration = 30.seconds,
    public val intervaloKeepalive: Duration? = 30.seconds,
    public val intentosKeepalive: Int = INTENTOS_KEEPALIVE,
    public val limiteBytesRekey: Long = LIMITE_BYTES_REKEY,
    public val limiteTiempoRekey: Duration = 1.hours,
    public val algoritmosPermitidos: Set<SshAlgorithm> = SshAlgorithm.entries.toSet(),
) {
    private companion object {
        const val PUERTO_SSH: Int = 22
        const val INTENTOS_KEEPALIVE: Int = 3
        const val LIMITE_BYTES_REKEY: Long = 1L shl 30
    }
}
