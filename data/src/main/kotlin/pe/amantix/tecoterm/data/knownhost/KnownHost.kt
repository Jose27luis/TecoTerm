package pe.amantix.tecoterm.data.knownhost

public data class KnownHost(
    public val host: String,
    public val puerto: Int,
    public val tipoClave: String,
    public val huella: String,
    public val vistoPorPrimeraVez: Long,
)
