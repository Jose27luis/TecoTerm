package pe.amantix.tecoterm.core.ssh.transport

import pe.amantix.tecoterm.core.ssh.crypto.AlgorithmCategory
import pe.amantix.tecoterm.core.ssh.crypto.SshAlgorithm
import kotlin.time.Duration

internal class TransportOptions(
    val host: String,
    val puerto: Int,
    val verificador: HostKeyVerifier,
    val algoritmos: (AlgorithmCategory) -> List<SshAlgorithm>,
    val limiteBytesRekey: Long,
    val limiteTiempoRekey: Duration,
)
