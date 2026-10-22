package pe.amantix.tecoterm.core.ssh.transport

import pe.amantix.tecoterm.core.ssh.wire.SshWriter
import java.security.MessageDigest

internal class ExchangeHash(
    private val versionCliente: String,
    private val versionServidor: String,
    private val kexInitCliente: ByteArray,
    private val kexInitServidor: ByteArray,
) {
    fun calcular(
        claveHost: ByteArray,
        publicaCliente: ByteArray,
        publicaServidor: ByteArray,
        secreto: ByteArray,
    ): ByteArray {
        val escritor =
            SshWriter()
                .escribirTexto(versionCliente)
                .escribirTexto(versionServidor)
                .escribirString(kexInitCliente)
                .escribirString(kexInitServidor)
                .escribirString(claveHost)
                .escribirString(publicaCliente)
                .escribirString(publicaServidor)
                .escribirMpintSinSigno(secreto)
        val datos = escritor.bytes()
        escritor.limpiar()
        val resumen = MessageDigest.getInstance("SHA-256").digest(datos)
        datos.fill(0)
        return resumen
    }
}
