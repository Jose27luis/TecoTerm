package pe.amantix.tecoterm.core.ssh.connection

public sealed interface ExitStatus {
    public data class Codigo(
        public val valor: Int,
    ) : ExitStatus

    public data class Senal(
        public val nombre: String,
        public val mensaje: String,
    ) : ExitStatus

    public data object SinEstado : ExitStatus
}
