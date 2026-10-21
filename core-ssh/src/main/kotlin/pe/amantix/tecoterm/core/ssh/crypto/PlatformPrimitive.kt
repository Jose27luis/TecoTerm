package pe.amantix.tecoterm.core.ssh.crypto

import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac

public sealed interface PlatformPrimitive {
    public val nombre: String

    public fun estaDisponible(): Boolean

    public data class KeyPairGeneratorPrimitive(
        override val nombre: String,
    ) : PlatformPrimitive {
        override fun estaDisponible(): Boolean = existe { KeyPairGenerator.getInstance(nombre) }
    }

    public data class KeyAgreementPrimitive(
        override val nombre: String,
    ) : PlatformPrimitive {
        override fun estaDisponible(): Boolean = existe { KeyAgreement.getInstance(nombre) }
    }

    public data class KeyFactoryPrimitive(
        override val nombre: String,
    ) : PlatformPrimitive {
        override fun estaDisponible(): Boolean = existe { KeyFactory.getInstance(nombre) }
    }

    public data class SignaturePrimitive(
        override val nombre: String,
    ) : PlatformPrimitive {
        override fun estaDisponible(): Boolean = existe { Signature.getInstance(nombre) }
    }

    public data class CipherPrimitive(
        override val nombre: String,
    ) : PlatformPrimitive {
        override fun estaDisponible(): Boolean = existe { Cipher.getInstance(nombre) }
    }

    public data class MacPrimitive(
        override val nombre: String,
    ) : PlatformPrimitive {
        override fun estaDisponible(): Boolean = existe { Mac.getInstance(nombre) }
    }

    public data class DigestPrimitive(
        override val nombre: String,
    ) : PlatformPrimitive {
        override fun estaDisponible(): Boolean = existe { MessageDigest.getInstance(nombre) }
    }
}

private inline fun existe(obtenerInstancia: () -> Unit): Boolean =
    try {
        obtenerInstancia()
        true
    } catch (_: GeneralSecurityException) {
        false
    }
