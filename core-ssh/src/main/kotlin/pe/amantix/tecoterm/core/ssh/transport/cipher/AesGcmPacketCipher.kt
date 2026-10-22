package pe.amantix.tecoterm.core.ssh.transport.cipher

import pe.amantix.tecoterm.core.ssh.SshProtocolException
import java.io.DataInputStream
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class AesGcmPacketCipher(
    clave: ByteArray,
    iv: ByteArray,
    private val aleatorio: SecureRandom,
) : PacketCipher {
    private val especificacion: SecretKeySpec = SecretKeySpec(clave, "AES")
    private val nonce: ByteArray = iv.copyOf()
    private val cifrador: Cipher = Cipher.getInstance("AES/GCM/NoPadding")

    init {
        require(nonce.size == LONGITUD_NONCE) { "El nonce de AES-GCM debe tener $LONGITUD_NONCE bytes" }
    }

    override fun empaquetar(
        payload: ByteArray,
        secuencia: Int,
    ): ByteArray {
        val plano = PacketLayout.construir(payload, BLOQUE, alineacionIncluyeLongitud = false, aleatorio = aleatorio)
        cifrador.init(Cipher.ENCRYPT_MODE, especificacion, GCMParameterSpec(BITS_ETIQUETA, nonce))
        cifrador.updateAAD(plano, 0, PacketLayout.BYTES_LONGITUD)
        val cifrado = cifrador.doFinal(plano, PacketLayout.BYTES_LONGITUD, plano.size - PacketLayout.BYTES_LONGITUD)
        avanzarNonce()
        val resultado = plano.copyOf(PacketLayout.BYTES_LONGITUD) + cifrado
        plano.fill(0)
        return resultado
    }

    override fun leer(
        entrada: DataInputStream,
        secuencia: Int,
    ): ByteArray {
        val cabecera = ByteArray(PacketLayout.BYTES_LONGITUD)
        entrada.readFully(cabecera)
        val longitud = PacketLayout.leerLongitud(cabecera, BLOQUE, alineacionIncluyeLongitud = false)
        val cifrado = ByteArray(longitud + BYTES_ETIQUETA)
        entrada.readFully(cifrado)
        val plano =
            try {
                cifrador.init(Cipher.DECRYPT_MODE, especificacion, GCMParameterSpec(BITS_ETIQUETA, nonce))
                cifrador.updateAAD(cabecera)
                cifrador.doFinal(cifrado)
            } catch (error: GeneralSecurityException) {
                throw SshProtocolException("La etiqueta de integridad del paquete no coincide", error)
            }
        avanzarNonce()
        val payload = PacketLayout.extraerPayload(plano, 0)
        plano.fill(0)
        return payload
    }

    private fun avanzarNonce() {
        var indice = nonce.size - 1
        while (indice >= INICIO_CONTADOR) {
            nonce[indice] = (nonce[indice] + 1).toByte()
            if (nonce[indice].toInt() != 0) {
                return
            }
            indice--
        }
    }

    private companion object {
        const val BLOQUE: Int = 16
        const val LONGITUD_NONCE: Int = 12
        const val INICIO_CONTADOR: Int = 4
        const val BYTES_ETIQUETA: Int = 16
        const val BITS_ETIQUETA: Int = 128
    }
}
