package pe.amantix.tecoterm.core.ssh.auth

import pe.amantix.tecoterm.core.ssh.SshProtocolException
import pe.amantix.tecoterm.core.ssh.crypto.SecretText
import pe.amantix.tecoterm.core.ssh.wire.SshMessage
import pe.amantix.tecoterm.core.ssh.wire.SshReader
import pe.amantix.tecoterm.core.ssh.wire.SshWriter

internal class KeyboardInteractiveMethod : AuthMethod {
    override val nombre: String = "keyboard-interactive"

    override suspend fun intentar(contexto: AuthContext): AuthOutcome {
        contexto.transport.enviar(
            contexto
                .solicitud(nombre)
                .escribirTexto("")
                .escribirTexto("")
                .bytes(),
        )
        while (true) {
            val paquete = contexto.recibir()
            when (paquete.tipo) {
                SshMessage.SSH_MSG_USERAUTH_INFO_REQUEST -> responder(contexto, leerDesafio(paquete.payload))
                SshMessage.SSH_MSG_USERAUTH_SUCCESS -> return AuthOutcome.Exito
                SshMessage.SSH_MSG_USERAUTH_FAILURE -> return contexto.fallo(paquete)
                else -> throw SshProtocolException("Respuesta inesperada en keyboard-interactive: ${paquete.tipo}")
            }
        }
    }

    private suspend fun responder(
        contexto: AuthContext,
        desafio: KeyboardInteractiveChallenge,
    ) {
        val respuestas = if (desafio.preguntas.isEmpty()) emptyList() else contexto.prompter.responder(desafio)
        try {
            if (respuestas.size != desafio.preguntas.size) {
                throw SshProtocolException("Se esperaban ${desafio.preguntas.size} respuestas y llegaron ${respuestas.size}")
            }
            val escritor = SshWriter().escribirByte(SshMessage.SSH_MSG_USERAUTH_INFO_RESPONSE).escribirUint32(respuestas.size)
            respuestas.forEach { respuesta ->
                val bytes = SecretText.utf8(respuesta)
                escritor.escribirString(bytes)
                bytes.fill(0)
            }
            val mensaje = escritor.bytes()
            escritor.limpiar()
            try {
                contexto.transport.enviar(mensaje)
            } finally {
                mensaje.fill(0)
            }
        } finally {
            respuestas.forEach { it.fill('\u0000') }
        }
    }

    private fun leerDesafio(payload: ByteArray): KeyboardInteractiveChallenge {
        val lector = SshReader(payload)
        lector.leerByte()
        val nombreDesafio = lector.leerTexto()
        val instruccion = lector.leerTexto()
        lector.leerTexto()
        val cantidad = lector.leerUint32()
        if (cantidad > MAXIMO_PREGUNTAS) {
            throw SshProtocolException("Demasiadas preguntas en keyboard-interactive: $cantidad")
        }
        val preguntas =
            List(cantidad.toInt()) {
                KeyboardInteractivePrompt(texto = lector.leerTexto(), mostrarRespuesta = lector.leerBoolean())
            }
        lector.verificarFin()
        return KeyboardInteractiveChallenge(nombreDesafio, instruccion, preguntas)
    }

    private companion object {
        const val MAXIMO_PREGUNTAS: Long = 32
    }
}
