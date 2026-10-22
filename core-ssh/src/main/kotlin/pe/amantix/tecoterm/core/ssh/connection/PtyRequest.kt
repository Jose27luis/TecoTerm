package pe.amantix.tecoterm.core.ssh.connection

public data class PtyRequest(
    public val termino: String = "xterm-256color",
    public val columnas: Int = 80,
    public val filas: Int = 24,
    public val anchoPixeles: Int = 0,
    public val altoPixeles: Int = 0,
)
