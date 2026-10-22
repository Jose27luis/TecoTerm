package pe.amantix.tecoterm.core.ssh.crypto

import pe.amantix.tecoterm.core.ssh.SshProtocolException
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

internal object RawKeyCodec {
    const val LONGITUD_CLAVE: Int = 32
    private const val PREFIJO_DER_X25519: String = "302a300506032b656e032100"
    private const val PREFIJO_DER_ED25519: String = "302a300506032b6570032100"
    private const val BASE_HEXADECIMAL: Int = 16

    private val prefijoX25519: ByteArray = desdeHexadecimal(PREFIJO_DER_X25519)
    private val prefijoEd25519: ByteArray = desdeHexadecimal(PREFIJO_DER_ED25519)

    fun clavePublicaX25519(crudo: ByteArray): PublicKey = clavePublica("X25519", prefijoX25519, crudo)

    fun clavePublicaEd25519(crudo: ByteArray): PublicKey = clavePublica("Ed25519", prefijoEd25519, crudo)

    fun crudoX25519(clave: PublicKey): ByteArray = crudo(prefijoX25519, clave)

    fun crudoEd25519(clave: PublicKey): ByteArray = crudo(prefijoEd25519, clave)

    fun esEd25519(clave: PublicKey): Boolean = tienePrefijo(prefijoEd25519, clave.encoded)

    private fun clavePublica(
        algoritmo: String,
        prefijo: ByteArray,
        crudo: ByteArray,
    ): PublicKey {
        if (crudo.size != LONGITUD_CLAVE) {
            throw SshProtocolException("Clave $algoritmo de ${crudo.size} bytes, se esperaban $LONGITUD_CLAVE")
        }
        return try {
            KeyFactory.getInstance(algoritmo).generatePublic(X509EncodedKeySpec(prefijo + crudo))
        } catch (error: GeneralSecurityException) {
            throw SshProtocolException("Clave $algoritmo inválida", error)
        }
    }

    private fun crudo(
        prefijo: ByteArray,
        clave: PublicKey,
    ): ByteArray {
        val codificada = clave.encoded
        if (!tienePrefijo(prefijo, codificada)) {
            throw SshProtocolException("Codificación de clave pública inesperada")
        }
        return codificada.copyOfRange(prefijo.size, codificada.size)
    }

    private fun tienePrefijo(
        prefijo: ByteArray,
        codificada: ByteArray,
    ): Boolean =
        codificada.size == prefijo.size + LONGITUD_CLAVE &&
            prefijo.indices.all { codificada[it] == prefijo[it] }

    private fun desdeHexadecimal(texto: String): ByteArray = texto.chunked(2).map { it.toInt(BASE_HEXADECIMAL).toByte() }.toByteArray()
}
