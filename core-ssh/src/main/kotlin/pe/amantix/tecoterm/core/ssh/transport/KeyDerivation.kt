package pe.amantix.tecoterm.core.ssh.transport

import pe.amantix.tecoterm.core.ssh.wire.SshWriter
import java.security.MessageDigest

internal class KeyDerivation(
    secreto: ByteArray,
    private val hash: ByteArray,
    private val idSesion: ByteArray,
) {
    private val secretoCodificado: ByteArray =
        SshWriter().run {
            escribirMpintSinSigno(secreto)
            val bytes = bytes()
            limpiar()
            bytes
        }

    fun derivar(
        letra: Char,
        longitud: Int,
    ): ByteArray {
        val resumen = MessageDigest.getInstance("SHA-256")
        resumen.update(secretoCodificado)
        resumen.update(hash)
        resumen.update(letra.code.toByte())
        resumen.update(idSesion)
        var resultado = resumen.digest()
        while (resultado.size < longitud) {
            resumen.update(secretoCodificado)
            resumen.update(hash)
            resumen.update(resultado)
            val siguiente = resultado + resumen.digest()
            resultado.fill(0)
            resultado = siguiente
        }
        val clave = resultado.copyOf(longitud)
        resultado.fill(0)
        return clave
    }

    fun destruir() {
        secretoCodificado.fill(0)
    }
}
