package pe.amantix.tecoterm.core.ssh.auth

import pe.amantix.tecoterm.core.ssh.SshProtocolException
import pe.amantix.tecoterm.core.ssh.transport.IncomingPacket
import pe.amantix.tecoterm.core.ssh.transport.Transport
import pe.amantix.tecoterm.core.ssh.wire.SshMessage
import pe.amantix.tecoterm.core.ssh.wire.SshReader
import pe.amantix.tecoterm.core.ssh.wire.SshWriter

internal class AuthContext(
    val transport: Transport,
    val usuario: String,
    val prompter: AuthenticationPrompter,
) {
    fun solicitud(metodo: String): SshWriter =
        SshWriter()
            .escribirByte(SshMessage.SSH_MSG_USERAUTH_REQUEST)
            .escribirTexto(usuario)
            .escribirTexto(SERVICIO_CONEXION)
            .escribirTexto(metodo)

    suspend fun recibir(): IncomingPacket {
        while (true) {
            val paquete = transport.recibir()
            if (paquete.tipo != SshMessage.SSH_MSG_USERAUTH_BANNER) {
                return paquete
            }
            val lector = SshReader(paquete.payload)
            lector.leerByte()
            prompter.mostrarBanner(lector.leerTexto())
        }
    }

    suspend fun recibirResultado(): AuthOutcome {
        val paquete = recibir()
        return when (paquete.tipo) {
            SshMessage.SSH_MSG_USERAUTH_SUCCESS -> AuthOutcome.Exito
            SshMessage.SSH_MSG_USERAUTH_FAILURE -> fallo(paquete)
            else -> throw SshProtocolException("Respuesta de autenticación inesperada: ${paquete.tipo}")
        }
    }

    fun fallo(paquete: IncomingPacket): AuthOutcome.Fallo {
        val lector = SshReader(paquete.payload)
        lector.leerByte()
        val metodos = lector.leerNameList()
        lector.leerBoolean()
        lector.verificarFin()
        return AuthOutcome.Fallo(metodos)
    }

    companion object {
        const val SERVICIO_AUTENTICACION: String = "ssh-userauth"
        const val SERVICIO_CONEXION: String = "ssh-connection"
    }
}
