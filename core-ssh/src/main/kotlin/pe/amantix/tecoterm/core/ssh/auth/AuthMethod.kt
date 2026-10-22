package pe.amantix.tecoterm.core.ssh.auth

internal interface AuthMethod {
    val nombre: String

    fun puedeIntentar(contexto: AuthContext): Boolean = true

    suspend fun intentar(contexto: AuthContext): AuthOutcome
}
