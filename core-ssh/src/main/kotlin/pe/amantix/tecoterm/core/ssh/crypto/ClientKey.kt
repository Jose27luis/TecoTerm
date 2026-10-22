package pe.amantix.tecoterm.core.ssh.crypto

import pe.amantix.tecoterm.core.ssh.InvalidKeyFileException
import pe.amantix.tecoterm.core.ssh.wire.SshWriter
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.KeyPair
import java.security.PrivateKey
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

public class ClientKey private constructor(
    private val clavePrivada: PrivateKey,
    public val tipo: String,
    private val blob: ByteArray,
    internal val algoritmosFirma: List<String>,
) {
    public val blobPublico: ByteArray
        get() = blob.copyOf()

    public fun huellaSha256(): String = HostKey.desdeBlob(blob).huellaSha256()

    public fun lineaPublica(comentario: String): String = "$tipo ${Base64.getEncoder().encodeToString(blob)} $comentario".trimEnd()

    public fun pkcs8(): ByteArray = checkNotNull(clavePrivada.encoded) { "La clave privada no se puede exportar" }

    internal fun firmar(
        algoritmoFirma: String,
        datos: ByteArray,
    ): ByteArray {
        val firma =
            when (algoritmoFirma) {
                Ed25519HostKey.TIPO -> {
                    firmarConPlataforma("Ed25519", datos)
                }

                EcdsaP256HostKey.TIPO -> {
                    val (r, s) = EcdsaSignatureCodec.rsDesdeDer(firmarConPlataforma("SHA256withECDSA", datos))
                    SshWriter().escribirMpint(r).escribirMpint(s).bytes()
                }

                else -> {
                    val algoritmo =
                        RsaHostKey.algoritmosFirma[algoritmoFirma]
                            ?: throw IllegalArgumentException("Algoritmo de firma no soportado: $algoritmoFirma")
                    firmarConPlataforma(algoritmo, datos)
                }
            }
        return SshWriter().escribirTexto(algoritmoFirma).escribirString(firma).bytes()
    }

    private fun firmarConPlataforma(
        algoritmo: String,
        datos: ByteArray,
    ): ByteArray =
        Signature.getInstance(algoritmo).run {
            initSign(clavePrivada)
            update(datos)
            sign()
        }

    public companion object {
        public fun desdeAlmacen(
            blobPublico: ByteArray,
            pkcs8: ByteArray,
        ): ClientKey {
            val publica = HostKey.desdeBlob(blobPublico)
            val privada =
                try {
                    KeyFactory.getInstance(publica.algoritmoPlataforma).generatePrivate(PKCS8EncodedKeySpec(pkcs8))
                } catch (error: GeneralSecurityException) {
                    throw InvalidKeyFileException("La clave guardada no se pudo reconstruir", error)
                }
            return desde(KeyPair(publica.clavePublica, privada))
        }

        public fun desde(par: KeyPair): ClientKey {
            val publica = par.public
            return when {
                RawKeyCodec.esEd25519(publica) -> {
                    ClientKey(
                        par.private,
                        Ed25519HostKey.TIPO,
                        SshWriter().escribirTexto(Ed25519HostKey.TIPO).escribirString(RawKeyCodec.crudoEd25519(publica)).bytes(),
                        listOf(Ed25519HostKey.TIPO),
                    )
                }

                publica is RSAPublicKey -> {
                    require(publica.modulus.bitLength() >= RsaHostKey.BITS_MINIMOS) {
                        "La clave RSA debe tener al menos ${RsaHostKey.BITS_MINIMOS} bits"
                    }
                    ClientKey(
                        par.private,
                        RsaHostKey.TIPO,
                        SshWriter()
                            .escribirTexto(RsaHostKey.TIPO)
                            .escribirMpint(publica.publicExponent)
                            .escribirMpint(publica.modulus)
                            .bytes(),
                        RsaHostKey.algoritmosFirma.keys.toList(),
                    )
                }

                publica is ECPublicKey && EcP256.esP256(publica) -> {
                    ClientKey(
                        par.private,
                        EcdsaP256HostKey.TIPO,
                        SshWriter()
                            .escribirTexto(EcdsaP256HostKey.TIPO)
                            .escribirTexto(EcP256.NOMBRE_CURVA)
                            .escribirString(EcP256.bytesDesdePunto(publica.w))
                            .bytes(),
                        listOf(EcdsaP256HostKey.TIPO),
                    )
                }

                else -> {
                    throw IllegalArgumentException("Tipo de clave no soportado: ${publica.algorithm}")
                }
            }
        }
    }
}
