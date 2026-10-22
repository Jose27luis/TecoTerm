package pe.amantix.tecoterm.core.ssh.transport

import pe.amantix.tecoterm.core.ssh.ServerDisconnectedException
import pe.amantix.tecoterm.core.ssh.wire.SshMessage
import pe.amantix.tecoterm.core.ssh.wire.SshReader
import pe.amantix.tecoterm.core.ssh.wire.SshWriter

internal object TransportMessages {
    private const val SERVER_SIG_ALGS: String = "server-sig-algs"

    fun desconexion(payload: ByteArray): ServerDisconnectedException {
        val lector = SshReader(payload)
        lector.leerByte()
        val codigo = lector.leerUint32().toInt()
        val descripcion = lector.leerTexto()
        return ServerDisconnectedException(codigo, descripcion)
    }

    fun crearDesconexion(
        codigo: Int,
        descripcion: String,
    ): ByteArray =
        SshWriter()
            .escribirByte(SshMessage.SSH_MSG_DISCONNECT)
            .escribirUint32(codigo)
            .escribirTexto(descripcion)
            .escribirTexto("")
            .bytes()

    fun algoritmosFirmaServidor(payload: ByteArray): Set<String>? {
        val lector = SshReader(payload)
        lector.leerByte()
        val cantidad = lector.leerUint32()
        var algoritmos: Set<String>? = null
        var restantes = cantidad
        while (restantes > 0 && lector.restantes > 0) {
            val nombre = lector.leerTexto()
            val valor = lector.leerString()
            if (nombre == SERVER_SIG_ALGS) {
                algoritmos = String(valor, Charsets.US_ASCII).split(',').filter(String::isNotEmpty).toSet()
            }
            restantes--
        }
        return algoritmos
    }
}
