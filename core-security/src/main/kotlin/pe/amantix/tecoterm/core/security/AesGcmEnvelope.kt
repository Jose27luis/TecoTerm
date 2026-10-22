package pe.amantix.tecoterm.core.security

import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class AesGcmEnvelope(
    private val clave: () -> SecretKey,
) {
    fun cifrar(
        plano: ByteArray,
        asociados: ByteArray,
    ): ByteArray =
        try {
            val cifrador = Cipher.getInstance(TRANSFORMACION)
            cifrador.init(Cipher.ENCRYPT_MODE, clave())
            cifrador.updateAAD(asociados)
            val cifrado = cifrador.doFinal(plano)
            val nonce = cifrador.iv
            if (nonce.size != LONGITUD_NONCE) {
                throw CredentialUnavailableException("El almacén seguro devolvió un nonce de ${nonce.size} bytes")
            }
            byteArrayOf(VERSION) + nonce + cifrado
        } catch (error: GeneralSecurityException) {
            throw CredentialUnavailableException("No se pudo cifrar la credencial", error)
        }

    fun descifrar(
        sobre: ByteArray,
        asociados: ByteArray,
    ): ByteArray {
        if (sobre.size < 1 + LONGITUD_NONCE + BYTES_ETIQUETA || sobre[0] != VERSION) {
            throw CredentialUnavailableException("El contenido cifrado tiene un formato desconocido")
        }
        return try {
            val cifrador = Cipher.getInstance(TRANSFORMACION)
            cifrador.init(Cipher.DECRYPT_MODE, clave(), GCMParameterSpec(BITS_ETIQUETA, sobre, 1, LONGITUD_NONCE))
            cifrador.updateAAD(asociados)
            cifrador.doFinal(sobre, 1 + LONGITUD_NONCE, sobre.size - 1 - LONGITUD_NONCE)
        } catch (error: GeneralSecurityException) {
            throw CredentialUnavailableException("La credencial no se pudo descifrar", error)
        }
    }

    private companion object {
        const val TRANSFORMACION: String = "AES/GCM/NoPadding"
        const val VERSION: Byte = 1
        const val LONGITUD_NONCE: Int = 12
        const val BYTES_ETIQUETA: Int = 16
        const val BITS_ETIQUETA: Int = 128
    }
}
