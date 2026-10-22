package pe.amantix.tecoterm.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory

internal class KeystoreKeyProvider(
    private val alias: String = ALIAS,
) {
    @Synchronized
    fun obtener(): SecretKey =
        try {
            val almacen = KeyStore.getInstance(PROVEEDOR).apply { load(null) }
            almacen.getKey(alias, null) as? SecretKey ?: crear()
        } catch (error: GeneralSecurityException) {
            throw CredentialUnavailableException("El Android Keystore no está disponible", error)
        }

    fun usaStrongBox(): Boolean {
        val clave = obtener()
        val informacion = SecretKeyFactory.getInstance(clave.algorithm, PROVEEDOR).getKeySpec(clave, KeyInfo::class.java) as? KeyInfo
        return informacion?.securityLevel == KeyProperties.SECURITY_LEVEL_STRONGBOX
    }

    private fun crear(): SecretKey =
        try {
            generar(conStrongBox = true)
        } catch (_: StrongBoxUnavailableException) {
            generar(conStrongBox = false)
        }

    private fun generar(conStrongBox: Boolean): SecretKey {
        val especificacion =
            KeyGenParameterSpec
                .Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(BITS_CLAVE)
                .setRandomizedEncryptionRequired(true)
                .setIsStrongBoxBacked(conStrongBox)
                .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVEEDOR).run {
            init(especificacion)
            generateKey()
        }
    }

    companion object {
        const val ALIAS: String = "tecoterm.credenciales"
        private const val PROVEEDOR: String = "AndroidKeyStore"
        private const val BITS_CLAVE: Int = 256
    }
}
