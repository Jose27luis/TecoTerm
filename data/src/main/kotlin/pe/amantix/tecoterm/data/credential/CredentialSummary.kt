package pe.amantix.tecoterm.data.credential

public data class CredentialSummary(
    public val id: String,
    public val tipo: CredentialKind,
    public val nombre: String,
    public val tipoClave: String?,
    public val huella: String?,
    public val lineaPublica: String?,
    public val creadaEn: Long,
)
