package pe.amantix.tecoterm.core.security

public class CredentialCipher internal constructor(
    private val sobre: AesGcmEnvelope,
    public val aliasKeystore: String,
) {
    public constructor() : this(AesGcmEnvelope(KeystoreKeyProvider()::obtener), KeystoreKeyProvider.ALIAS)

    public fun cifrarYBorrar(
        plano: ByteArray,
        idCredencial: String,
    ): ByteArray =
        try {
            sobre.cifrar(plano, asociados(idCredencial))
        } finally {
            plano.fill(0)
        }

    public fun descifrar(
        contenido: ByteArray,
        idCredencial: String,
    ): ByteArray = sobre.descifrar(contenido, asociados(idCredencial))

    private fun asociados(idCredencial: String): ByteArray = "$PREFIJO_ASOCIADOS$idCredencial".toByteArray(Charsets.UTF_8)

    private companion object {
        const val PREFIJO_ASOCIADOS: String = "tecoterm.credencial:"
    }
}
