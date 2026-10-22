package pe.amantix.tecoterm.core.ssh.crypto

import pe.amantix.tecoterm.core.ssh.SshProtocolException
import pe.amantix.tecoterm.core.ssh.wire.SshReader
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature
import java.util.Base64

public sealed class HostKey(
    blob: ByteArray,
) {
    private val contenido: ByteArray = blob.copyOf()

    public val blob: ByteArray
        get() = contenido.copyOf()

    public abstract val tipo: String

    internal abstract val clavePublica: PublicKey

    internal abstract val algoritmoPlataforma: String

    public fun huellaSha256(): String {
        val resumen = MessageDigest.getInstance("SHA-256").digest(contenido)
        return "SHA256:" + Base64.getEncoder().withoutPadding().encodeToString(resumen)
    }

    internal abstract fun admite(algoritmoFirma: String): Boolean

    internal abstract fun verificarFirma(
        algoritmoFirma: String,
        datos: ByteArray,
        firma: ByteArray,
    ): Boolean

    internal fun verificar(
        algoritmoFirma: String,
        datos: ByteArray,
        blobFirma: ByteArray,
    ): Boolean {
        if (!admite(algoritmoFirma)) {
            return false
        }
        val lector = SshReader(blobFirma)
        val nombre = String(lector.leerString(), Charsets.US_ASCII)
        val firma = lector.leerString()
        lector.verificarFin()
        return nombre == algoritmoFirma && verificarFirma(algoritmoFirma, datos, firma)
    }

    override fun equals(other: Any?): Boolean = other is HostKey && MessageDigest.isEqual(contenido, other.contenido)

    override fun hashCode(): Int = contenido.contentHashCode()

    public companion object {
        public fun desdeBlob(blob: ByteArray): HostKey {
            val lector = SshReader(blob)
            return when (val tipo = String(lector.leerString(), Charsets.US_ASCII)) {
                Ed25519HostKey.TIPO -> Ed25519HostKey.leer(blob, lector)
                RsaHostKey.TIPO -> RsaHostKey.leer(blob, lector)
                EcdsaP256HostKey.TIPO -> EcdsaP256HostKey.leer(blob, lector)
                else -> throw SshProtocolException("Tipo de clave de servidor no soportado: $tipo")
            }
        }

        internal fun verificarConPlataforma(
            algoritmo: String,
            clave: PublicKey,
            datos: ByteArray,
            firma: ByteArray,
        ): Boolean =
            try {
                Signature.getInstance(algoritmo).run {
                    initVerify(clave)
                    update(datos)
                    verify(firma)
                }
            } catch (_: GeneralSecurityException) {
                false
            }
    }
}
