package pe.amantix.tecoterm.core.ssh.auth

public data class KeyboardInteractiveChallenge(
    public val nombre: String,
    public val instruccion: String,
    public val preguntas: List<KeyboardInteractivePrompt>,
)

public data class KeyboardInteractivePrompt(
    public val texto: String,
    public val mostrarRespuesta: Boolean,
)
