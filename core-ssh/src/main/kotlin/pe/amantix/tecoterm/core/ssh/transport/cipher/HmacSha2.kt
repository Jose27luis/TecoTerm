package pe.amantix.tecoterm.core.ssh.transport.cipher

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal class HmacSha2(
    algoritmoPlataforma: String,
    clave: ByteArray,
) {
    private val mac: Mac =
        Mac.getInstance(algoritmoPlataforma).apply {
            init(SecretKeySpec(clave, algoritmoPlataforma))
        }

    val longitud: Int = mac.macLength

    fun calcular(
        secuencia: Int,
        datos: ByteArray,
    ): ByteArray {
        mac.update(PacketLayout.bytesSecuencia(secuencia))
        mac.update(datos)
        return mac.doFinal()
    }

    fun coincide(
        secuencia: Int,
        datos: ByteArray,
        recibida: ByteArray,
    ): Boolean = MessageDigest.isEqual(calcular(secuencia, datos), recibida)
}
