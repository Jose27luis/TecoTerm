package pe.amantix.tecoterm.core.ssh.transport

import pe.amantix.tecoterm.core.ssh.SshProtocolException
import pe.amantix.tecoterm.core.ssh.crypto.EcP256
import java.security.GeneralSecurityException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.ECPublicKey
import javax.crypto.KeyAgreement

internal class EcdhP256Exchange : KeyExchange {
    private val par: KeyPair =
        KeyPairGenerator.getInstance("EC").run {
            initialize(EcP256.parametros)
            generateKeyPair()
        }

    override val clavePublicaCliente: ByteArray =
        EcP256.bytesDesdePunto(
            (par.public as? ECPublicKey ?: throw SshProtocolException("La plataforma no generó una clave EC")).w,
        )

    override fun calcularSecreto(clavePublicaServidor: ByteArray): ByteArray {
        val claveServidor = EcP256.clavePublica(EcP256.puntoDesdeBytes(clavePublicaServidor))
        val secreto =
            try {
                KeyAgreement.getInstance("ECDH").run {
                    init(par.private)
                    doPhase(claveServidor, true)
                    generateSecret()
                }
            } catch (error: GeneralSecurityException) {
                throw SshProtocolException("Clave ECDH del servidor inválida", error)
            }
        KeyExchange.exigirSecretoNoNulo(secreto)
        return secreto
    }
}
