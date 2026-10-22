package pe.amantix.tecoterm.core.ssh.keys

import pe.amantix.tecoterm.core.ssh.PassphraseRequiredException

internal object KeyFileSecrets {
    const val LONGITUD_AES_128: Int = 16
    const val LONGITUD_AES_192: Int = 24
    const val LONGITUD_AES_256: Int = 32
    const val LONGITUD_IV_AES: Int = 16
    const val LONGITUD_NONCE_GCM: Int = 12

    fun exigirPassphrase(passphrase: CharArray?): CharArray {
        if (passphrase == null || passphrase.isEmpty()) {
            throw PassphraseRequiredException()
        }
        return passphrase
    }
}
