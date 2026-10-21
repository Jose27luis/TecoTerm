package pe.amantix.tecoterm.core.ssh.crypto

public object PlatformAlgorithms {
    public fun detectar(): List<AlgorithmAvailability> =
        SshAlgorithm.entries.map { algoritmo ->
            AlgorithmAvailability(
                algoritmo = algoritmo,
                primitivasFaltantes = algoritmo.primitivas.filterNot(PlatformPrimitive::estaDisponible),
            )
        }

    public fun disponibles(categoria: AlgorithmCategory): List<SshAlgorithm> =
        detectar()
            .filter { it.disponible && it.algoritmo.categoria == categoria }
            .map(AlgorithmAvailability::algoritmo)
}
