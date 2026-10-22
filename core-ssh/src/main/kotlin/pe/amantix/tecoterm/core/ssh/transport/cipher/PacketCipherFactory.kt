package pe.amantix.tecoterm.core.ssh.transport.cipher

import pe.amantix.tecoterm.core.ssh.SshProtocolException
import pe.amantix.tecoterm.core.ssh.crypto.SshAlgorithm
import pe.amantix.tecoterm.core.ssh.transport.DirectionAlgorithms
import pe.amantix.tecoterm.core.ssh.transport.KeyDerivation
import java.security.SecureRandom

internal class PacketCipherFactory(
    private val aleatorio: SecureRandom,
) {
    fun crear(
        algoritmos: DirectionAlgorithms,
        derivacion: KeyDerivation,
        letras: DerivationLetters,
    ): PacketCipher {
        val cifrado = algoritmos.cifrado
        val clave = derivacion.derivar(letras.clave, longitudClave(cifrado))
        return try {
            when (cifrado) {
                SshAlgorithm.AES256_GCM, SshAlgorithm.AES128_GCM -> {
                    AesGcmPacketCipher(clave, derivacion.derivar(letras.iv, LONGITUD_NONCE_GCM), aleatorio)
                }

                SshAlgorithm.AES256_CTR, SshAlgorithm.AES128_CTR -> {
                    val algoritmoMac = algoritmos.mac ?: throw SshProtocolException("AES-CTR necesita un algoritmo de integridad")
                    val claveMac = derivacion.derivar(letras.mac, longitudMac(algoritmoMac))
                    val mac = HmacSha2(nombreMac(algoritmoMac), claveMac)
                    claveMac.fill(0)
                    AesCtrPacketCipher(
                        clave = clave,
                        iv = derivacion.derivar(letras.iv, LONGITUD_IV_CTR),
                        mac = mac,
                        cifrarAntesDeMac = algoritmoMac == SshAlgorithm.HMAC_SHA2_256_ETM || algoritmoMac == SshAlgorithm.HMAC_SHA2_512_ETM,
                        aleatorio = aleatorio,
                    )
                }

                else -> {
                    throw SshProtocolException("${cifrado.nombre} no es un cifrado")
                }
            }
        } finally {
            clave.fill(0)
        }
    }

    private fun longitudClave(cifrado: SshAlgorithm): Int =
        when (cifrado) {
            SshAlgorithm.AES256_GCM, SshAlgorithm.AES256_CTR -> LONGITUD_AES_256
            else -> LONGITUD_AES_128
        }

    private fun longitudMac(mac: SshAlgorithm): Int =
        when (mac) {
            SshAlgorithm.HMAC_SHA2_512, SshAlgorithm.HMAC_SHA2_512_ETM -> LONGITUD_SHA_512
            else -> LONGITUD_SHA_256
        }

    private fun nombreMac(mac: SshAlgorithm): String =
        when (mac) {
            SshAlgorithm.HMAC_SHA2_512, SshAlgorithm.HMAC_SHA2_512_ETM -> "HmacSHA512"
            SshAlgorithm.HMAC_SHA2_256, SshAlgorithm.HMAC_SHA2_256_ETM -> "HmacSHA256"
            else -> throw SshProtocolException("${mac.nombre} no es un algoritmo de integridad")
        }

    private companion object {
        const val LONGITUD_AES_256: Int = 32
        const val LONGITUD_AES_128: Int = 16
        const val LONGITUD_NONCE_GCM: Int = 12
        const val LONGITUD_IV_CTR: Int = 16
        const val LONGITUD_SHA_256: Int = 32
        const val LONGITUD_SHA_512: Int = 64
    }
}

internal data class DerivationLetters(
    val iv: Char,
    val clave: Char,
    val mac: Char,
) {
    companion object {
        val clienteServidor: DerivationLetters = DerivationLetters('A', 'C', 'E')
        val servidorCliente: DerivationLetters = DerivationLetters('B', 'D', 'F')
    }
}
