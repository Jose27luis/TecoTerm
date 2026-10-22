package pe.amantix.tecoterm.core.security

import java.security.GeneralSecurityException
import java.security.SecureRandom

public object SecurityDiagnostics {
    private const val BYTES_PRUEBA: Int = 32
    private const val ID_PRUEBA: String = "diagnostico"

    public fun comprobar(): SecurityStatus {
        val proveedor = KeystoreKeyProvider()
        val sobre = AesGcmEnvelope(proveedor::obtener)
        val prueba = ByteArray(BYTES_PRUEBA).also(SecureRandom()::nextBytes)
        val asociados = ID_PRUEBA.toByteArray(Charsets.UTF_8)
        return try {
            val funciona = sobre.descifrar(sobre.cifrar(prueba, asociados), asociados).contentEquals(prueba)
            SecurityStatus(keystoreFunciona = funciona, strongBox = funciona && proveedor.usaStrongBox())
        } catch (_: CredentialUnavailableException) {
            SecurityStatus(keystoreFunciona = false, strongBox = false)
        } catch (_: GeneralSecurityException) {
            SecurityStatus(keystoreFunciona = false, strongBox = false)
        }
    }
}

public data class SecurityStatus(
    public val keystoreFunciona: Boolean,
    public val strongBox: Boolean,
)
