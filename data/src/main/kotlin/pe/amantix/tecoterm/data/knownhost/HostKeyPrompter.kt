package pe.amantix.tecoterm.data.knownhost

public interface HostKeyPrompter {
    public suspend fun confirmarNuevo(servidor: HostKeyPrompt): Boolean

    public suspend fun avisarCambio(cambio: HostKeyChange)
}

public data class HostKeyPrompt(
    public val host: String,
    public val puerto: Int,
    public val tipoClave: String,
    public val huella: String,
)

public data class HostKeyChange(
    public val host: String,
    public val puerto: Int,
    public val tipoAnterior: String,
    public val huellaAnterior: String,
    public val tipoNuevo: String,
    public val huellaNueva: String,
)
