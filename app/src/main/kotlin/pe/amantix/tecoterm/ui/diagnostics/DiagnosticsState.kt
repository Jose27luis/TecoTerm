package pe.amantix.tecoterm.ui.diagnostics

import pe.amantix.tecoterm.core.security.SecurityStatus
import pe.amantix.tecoterm.core.ssh.crypto.AlgorithmAvailability

public sealed interface DiagnosticsState {
    public data object Cargando : DiagnosticsState

    public data class Listo(
        public val algoritmos: List<AlgorithmAvailability>,
        public val seguridad: SecurityStatus,
    ) : DiagnosticsState
}
