package pe.amantix.tecoterm.core.ssh.auth

internal sealed interface AuthOutcome {
    data object Exito : AuthOutcome

    data class Fallo(
        val metodosPermitidos: List<String>,
    ) : AuthOutcome
}
