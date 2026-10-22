package pe.amantix.tecoterm.core.ssh.auth

public interface AuthenticationPrompter {
    public suspend fun responder(desafio: KeyboardInteractiveChallenge): List<CharArray>

    public fun mostrarBanner(mensaje: String)
}
