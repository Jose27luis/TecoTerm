package pe.amantix.tecoterm.core.ssh.crypto

import pe.amantix.tecoterm.core.ssh.SshProtocolException
import pe.amantix.tecoterm.core.ssh.wire.SshReader
import java.math.BigInteger
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec

public class RsaHostKey internal constructor(
    blob: ByteArray,
    internal val clave: RSAPublicKey,
) : HostKey(blob) {
    override val tipo: String = TIPO

    override fun admite(algoritmoFirma: String): Boolean = algoritmoFirma in algoritmosFirma.keys

    override fun verificarFirma(
        algoritmoFirma: String,
        datos: ByteArray,
        firma: ByteArray,
    ): Boolean {
        val algoritmo = algoritmosFirma[algoritmoFirma]
        val longitudModulo = (clave.modulus.bitLength() + BITS_BYTE - 1) / BITS_BYTE
        return algoritmo != null &&
            firma.size <= longitudModulo &&
            verificarConPlataforma(algoritmo, clave, datos, ByteArray(longitudModulo - firma.size) + firma)
    }

    internal companion object {
        const val TIPO: String = "ssh-rsa"
        const val BITS_MINIMOS: Int = 2048
        private const val BITS_BYTE: Int = 8

        val algoritmosFirma: Map<String, String> =
            mapOf(
                "rsa-sha2-512" to "SHA512withRSA",
                "rsa-sha2-256" to "SHA256withRSA",
            )

        fun leer(
            blob: ByteArray,
            lector: SshReader,
        ): RsaHostKey {
            val exponente = lector.leerMpint()
            val modulo = lector.leerMpint()
            lector.verificarFin()
            return RsaHostKey(blob, crearClave(modulo, exponente))
        }

        private fun crearClave(
            modulo: BigInteger,
            exponente: BigInteger,
        ): RSAPublicKey {
            val clave =
                if (modulo.bitLength() >= BITS_MINIMOS && exponente.signum() > 0) {
                    try {
                        KeyFactory.getInstance("RSA").generatePublic(RSAPublicKeySpec(modulo, exponente))
                    } catch (error: GeneralSecurityException) {
                        throw SshProtocolException("Clave RSA del servidor inválida", error)
                    }
                } else {
                    null
                }
            return clave as? RSAPublicKey
                ?: throw SshProtocolException("Clave RSA del servidor menor a $BITS_MINIMOS bits o inválida")
        }
    }
}
