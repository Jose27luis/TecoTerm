package pe.amantix.tecoterm.core.ssh.keys

import pe.amantix.tecoterm.core.ssh.InvalidKeyFileException
import pe.amantix.tecoterm.core.ssh.crypto.ClientKey
import pe.amantix.tecoterm.core.ssh.crypto.EcP256
import pe.amantix.tecoterm.core.ssh.crypto.HostKey
import pe.amantix.tecoterm.core.ssh.crypto.RawKeyCodec
import java.math.BigInteger
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.ECPrivateKeySpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPrivateCrtKeySpec
import java.security.spec.RSAPublicKeySpec

internal object KeyPairFactory {
    private const val PREFIJO_PKCS8_ED25519: String = "302e020100300506032b657004220420"
    private const val BASE_HEXADECIMAL: Int = 16
    private val mensajePrueba: ByteArray = "tecoterm-validacion-de-clave".toByteArray(Charsets.US_ASCII)

    fun ed25519(
        semilla: ByteArray,
        publica: ByteArray,
    ): KeyPair {
        if (semilla.size != RawKeyCodec.LONGITUD_CLAVE) {
            throw InvalidKeyFileException("Semilla Ed25519 de ${semilla.size} bytes")
        }
        val pkcs8 = desdeHexadecimal(PREFIJO_PKCS8_ED25519) + semilla
        try {
            return generar {
                KeyPair(
                    RawKeyCodec.clavePublicaEd25519(publica),
                    KeyFactory.getInstance("Ed25519").generatePrivate(PKCS8EncodedKeySpec(pkcs8)),
                )
            }
        } finally {
            pkcs8.fill(0)
        }
    }

    fun rsa(componentes: RsaComponents): KeyPair =
        generar {
            val fabrica = KeyFactory.getInstance("RSA")
            val unoMenos = { primo: BigInteger -> primo.subtract(BigInteger.ONE) }
            KeyPair(
                fabrica.generatePublic(RSAPublicKeySpec(componentes.modulo, componentes.exponentePublico)),
                fabrica.generatePrivate(
                    RSAPrivateCrtKeySpec(
                        componentes.modulo,
                        componentes.exponentePublico,
                        componentes.exponentePrivado,
                        componentes.primoP,
                        componentes.primoQ,
                        componentes.exponentePrivado.mod(unoMenos(componentes.primoP)),
                        componentes.exponentePrivado.mod(unoMenos(componentes.primoQ)),
                        componentes.primoQ.modInverse(componentes.primoP),
                    ),
                ),
            )
        }

    fun ecdsaP256(
        privado: BigInteger,
        punto: ByteArray,
    ): KeyPair =
        generar {
            KeyPair(
                EcP256.clavePublica(EcP256.puntoDesdeBytes(punto)),
                KeyFactory.getInstance("EC").generatePrivate(ECPrivateKeySpec(privado, EcP256.parametros)),
            )
        }

    fun validar(par: KeyPair): ClientKey {
        val clave =
            try {
                ClientKey.desde(par)
            } catch (error: IllegalArgumentException) {
                throw InvalidKeyFileException("Tipo de clave no soportado", error)
            }
        val algoritmo = clave.algoritmosFirma.first()
        if (!HostKey.desdeBlob(clave.blobPublico).verificar(algoritmo, mensajePrueba, clave.firmar(algoritmo, mensajePrueba))) {
            throw InvalidKeyFileException("La clave privada no corresponde a la pública")
        }
        return clave
    }

    private inline fun generar(crear: () -> KeyPair): KeyPair =
        try {
            crear()
        } catch (error: GeneralSecurityException) {
            throw InvalidKeyFileException("La clave privada no es válida", error)
        } catch (error: ArithmeticException) {
            throw InvalidKeyFileException("La clave RSA no es válida", error)
        }

    private fun desdeHexadecimal(texto: String): ByteArray = texto.chunked(2).map { it.toInt(BASE_HEXADECIMAL).toByte() }.toByteArray()
}

internal class RsaComponents(
    val modulo: BigInteger,
    val exponentePublico: BigInteger,
    val exponentePrivado: BigInteger,
    val primoP: BigInteger,
    val primoQ: BigInteger,
)
