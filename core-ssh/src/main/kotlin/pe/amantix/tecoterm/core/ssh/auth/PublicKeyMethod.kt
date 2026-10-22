package pe.amantix.tecoterm.core.ssh.auth

import pe.amantix.tecoterm.core.ssh.SshProtocolException
import pe.amantix.tecoterm.core.ssh.crypto.ClientKey
import pe.amantix.tecoterm.core.ssh.wire.SshMessage
import pe.amantix.tecoterm.core.ssh.wire.SshWriter

internal class PublicKeyMethod(
    private val clave: ClientKey,
) : AuthMethod {
    override val nombre: String = "publickey"

    override fun puedeIntentar(contexto: AuthContext): Boolean = algoritmoFirma(contexto) != null

    override suspend fun intentar(contexto: AuthContext): AuthOutcome {
        val algoritmo =
            algoritmoFirma(contexto) ?: throw SshProtocolException("El servidor no acepta ninguna firma de la clave ${clave.tipo}")
        val blob = clave.blobPublico
        contexto.transport.enviar(
            contexto
                .solicitud(nombre)
                .escribirBoolean(false)
                .escribirTexto(algoritmo)
                .escribirString(blob)
                .bytes(),
        )
        val consulta = contexto.recibir()
        when (consulta.tipo) {
            SshMessage.SSH_MSG_USERAUTH_FAILURE -> return contexto.fallo(consulta)
            SshMessage.SSH_MSG_USERAUTH_PK_OK -> Unit
            else -> throw SshProtocolException("Respuesta inesperada a la consulta de clave pública: ${consulta.tipo}")
        }
        val datosFirmados =
            SshWriter()
                .escribirString(contexto.transport.idSesion)
                .escribirBytes(
                    contexto
                        .solicitud(nombre)
                        .escribirBoolean(true)
                        .escribirTexto(algoritmo)
                        .escribirString(blob)
                        .bytes(),
                ).bytes()
        val firma = clave.firmar(algoritmo, datosFirmados)
        contexto.transport.enviar(
            contexto
                .solicitud(nombre)
                .escribirBoolean(true)
                .escribirTexto(algoritmo)
                .escribirString(blob)
                .escribirString(firma)
                .bytes(),
        )
        return contexto.recibirResultado()
    }

    private fun algoritmoFirma(contexto: AuthContext): String? {
        val firmasServidor = contexto.transport.algoritmosFirmaServidor
        return clave.algoritmosFirma.firstOrNull { firmasServidor.isEmpty() || it in firmasServidor }
    }
}
