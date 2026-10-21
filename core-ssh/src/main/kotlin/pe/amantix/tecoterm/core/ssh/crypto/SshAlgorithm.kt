package pe.amantix.tecoterm.core.ssh.crypto

import pe.amantix.tecoterm.core.ssh.crypto.PlatformPrimitive.CipherPrimitive
import pe.amantix.tecoterm.core.ssh.crypto.PlatformPrimitive.DigestPrimitive
import pe.amantix.tecoterm.core.ssh.crypto.PlatformPrimitive.KeyAgreementPrimitive
import pe.amantix.tecoterm.core.ssh.crypto.PlatformPrimitive.KeyFactoryPrimitive
import pe.amantix.tecoterm.core.ssh.crypto.PlatformPrimitive.KeyPairGeneratorPrimitive
import pe.amantix.tecoterm.core.ssh.crypto.PlatformPrimitive.MacPrimitive
import pe.amantix.tecoterm.core.ssh.crypto.PlatformPrimitive.SignaturePrimitive

public enum class SshAlgorithm(
    public val nombre: String,
    public val categoria: AlgorithmCategory,
    public val primitivas: List<PlatformPrimitive>,
) {
    CURVE25519_SHA256(
        "curve25519-sha256",
        AlgorithmCategory.INTERCAMBIO_CLAVES,
        listOf(KeyPairGeneratorPrimitive("X25519"), KeyAgreementPrimitive("X25519"), DigestPrimitive("SHA-256")),
    ),
    CURVE25519_SHA256_LIBSSH(
        "curve25519-sha256@libssh.org",
        AlgorithmCategory.INTERCAMBIO_CLAVES,
        listOf(KeyPairGeneratorPrimitive("X25519"), KeyAgreementPrimitive("X25519"), DigestPrimitive("SHA-256")),
    ),
    ECDH_SHA2_NISTP256(
        "ecdh-sha2-nistp256",
        AlgorithmCategory.INTERCAMBIO_CLAVES,
        listOf(KeyPairGeneratorPrimitive("EC"), KeyAgreementPrimitive("ECDH"), DigestPrimitive("SHA-256")),
    ),
    SSH_ED25519(
        "ssh-ed25519",
        AlgorithmCategory.CLAVE_HOST,
        listOf(KeyFactoryPrimitive("Ed25519"), SignaturePrimitive("Ed25519")),
    ),
    RSA_SHA2_512(
        "rsa-sha2-512",
        AlgorithmCategory.CLAVE_HOST,
        listOf(KeyFactoryPrimitive("RSA"), SignaturePrimitive("SHA512withRSA")),
    ),
    RSA_SHA2_256(
        "rsa-sha2-256",
        AlgorithmCategory.CLAVE_HOST,
        listOf(KeyFactoryPrimitive("RSA"), SignaturePrimitive("SHA256withRSA")),
    ),
    ECDSA_SHA2_NISTP256(
        "ecdsa-sha2-nistp256",
        AlgorithmCategory.CLAVE_HOST,
        listOf(KeyFactoryPrimitive("EC"), SignaturePrimitive("SHA256withECDSA")),
    ),
    AES256_GCM(
        "aes256-gcm@openssh.com",
        AlgorithmCategory.CIFRADO,
        listOf(CipherPrimitive("AES/GCM/NoPadding")),
    ),
    AES128_GCM(
        "aes128-gcm@openssh.com",
        AlgorithmCategory.CIFRADO,
        listOf(CipherPrimitive("AES/GCM/NoPadding")),
    ),
    AES256_CTR(
        "aes256-ctr",
        AlgorithmCategory.CIFRADO,
        listOf(CipherPrimitive("AES/CTR/NoPadding")),
    ),
    AES128_CTR(
        "aes128-ctr",
        AlgorithmCategory.CIFRADO,
        listOf(CipherPrimitive("AES/CTR/NoPadding")),
    ),
    HMAC_SHA2_256_ETM(
        "hmac-sha2-256-etm@openssh.com",
        AlgorithmCategory.MAC,
        listOf(MacPrimitive("HmacSHA256")),
    ),
    HMAC_SHA2_512_ETM(
        "hmac-sha2-512-etm@openssh.com",
        AlgorithmCategory.MAC,
        listOf(MacPrimitive("HmacSHA512")),
    ),
    HMAC_SHA2_256(
        "hmac-sha2-256",
        AlgorithmCategory.MAC,
        listOf(MacPrimitive("HmacSHA256")),
    ),
    HMAC_SHA2_512(
        "hmac-sha2-512",
        AlgorithmCategory.MAC,
        listOf(MacPrimitive("HmacSHA512")),
    ),
}
