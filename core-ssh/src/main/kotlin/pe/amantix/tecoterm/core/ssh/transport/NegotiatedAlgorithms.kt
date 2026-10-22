package pe.amantix.tecoterm.core.ssh.transport

import pe.amantix.tecoterm.core.ssh.crypto.SshAlgorithm

internal data class NegotiatedAlgorithms(
    val intercambio: SshAlgorithm,
    val claveHost: SshAlgorithm,
    val clienteServidor: DirectionAlgorithms,
    val servidorCliente: DirectionAlgorithms,
    val kexEstricto: Boolean,
)

internal data class DirectionAlgorithms(
    val cifrado: SshAlgorithm,
    val mac: SshAlgorithm?,
)
