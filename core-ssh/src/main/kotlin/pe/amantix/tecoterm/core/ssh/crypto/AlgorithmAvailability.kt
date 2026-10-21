package pe.amantix.tecoterm.core.ssh.crypto

public data class AlgorithmAvailability(
    public val algoritmo: SshAlgorithm,
    public val primitivasFaltantes: List<PlatformPrimitive>,
) {
    public val disponible: Boolean
        get() = primitivasFaltantes.isEmpty()
}
