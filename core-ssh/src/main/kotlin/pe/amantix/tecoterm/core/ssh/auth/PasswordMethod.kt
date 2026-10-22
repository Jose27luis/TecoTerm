package pe.amantix.tecoterm.core.ssh.auth

import pe.amantix.tecoterm.core.ssh.crypto.SecretText

internal class PasswordMethod(
    private val credencial: Credential.Password,
) : AuthMethod {
    override val nombre: String = "password"

    override suspend fun intentar(contexto: AuthContext): AuthOutcome {
        val contrasena = SecretText.utf8(credencial.contrasena)
        val escritor = contexto.solicitud(nombre).escribirBoolean(false).escribirString(contrasena)
        contrasena.fill(0)
        val solicitud = escritor.bytes()
        escritor.limpiar()
        try {
            contexto.transport.enviar(solicitud)
        } finally {
            solicitud.fill(0)
            credencial.borrar()
        }
        return contexto.recibirResultado()
    }
}
