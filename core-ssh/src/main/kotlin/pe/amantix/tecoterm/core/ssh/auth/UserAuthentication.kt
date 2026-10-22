package pe.amantix.tecoterm.core.ssh.auth

import pe.amantix.tecoterm.core.ssh.AuthenticationFailedException
import pe.amantix.tecoterm.core.ssh.SshProtocolException
import pe.amantix.tecoterm.core.ssh.transport.Transport
import pe.amantix.tecoterm.core.ssh.wire.SshMessage
import pe.amantix.tecoterm.core.ssh.wire.SshReader
import pe.amantix.tecoterm.core.ssh.wire.SshWriter

internal class UserAuthentication(
    transport: Transport,
    usuario: String,
    prompter: AuthenticationPrompter,
) {
    private val contexto: AuthContext = AuthContext(transport, usuario, prompter)

    suspend fun autenticar(credenciales: List<Credential>) {
        try {
            solicitarServicio()
            contexto.transport.enviar(contexto.solicitud(METODO_NINGUNO).bytes())
            var resultado = contexto.recibirResultado()
            val pendientes = ArrayDeque(credenciales.map(::metodoPara))
            while (resultado is AuthOutcome.Fallo) {
                val permitidos = resultado.metodosPermitidos
                val metodo = pendientes.removeFirstOrNull() ?: throw AuthenticationFailedException(permitidos)
                if (metodo.nombre in permitidos && metodo.puedeIntentar(contexto)) {
                    resultado = metodo.intentar(contexto)
                }
            }
        } finally {
            credenciales.filterIsInstance<Credential.Password>().forEach(Credential.Password::borrar)
        }
    }

    private suspend fun solicitarServicio() {
        contexto.transport.enviar(
            SshWriter()
                .escribirByte(SshMessage.SSH_MSG_SERVICE_REQUEST)
                .escribirTexto(AuthContext.SERVICIO_AUTENTICACION)
                .bytes(),
        )
        val respuesta = contexto.recibir()
        if (respuesta.tipo != SshMessage.SSH_MSG_SERVICE_ACCEPT) {
            throw SshProtocolException("El servidor no aceptó el servicio de autenticación")
        }
        val lector = SshReader(respuesta.payload)
        lector.leerByte()
        if (lector.leerTexto() != AuthContext.SERVICIO_AUTENTICACION) {
            throw SshProtocolException("El servidor aceptó un servicio distinto al pedido")
        }
    }

    private fun metodoPara(credencial: Credential): AuthMethod =
        when (credencial) {
            is Credential.Password -> PasswordMethod(credencial)
            is Credential.PublicKey -> PublicKeyMethod(credencial.clave)
            Credential.KeyboardInteractive -> KeyboardInteractiveMethod()
        }

    private companion object {
        const val METODO_NINGUNO: String = "none"
    }
}
