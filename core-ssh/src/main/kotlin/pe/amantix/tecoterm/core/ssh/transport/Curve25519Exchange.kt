package pe.amantix.tecoterm.core.ssh.transport

import pe.amantix.tecoterm.core.ssh.SshProtocolException
import pe.amantix.tecoterm.core.ssh.crypto.RawKeyCodec
import java.security.GeneralSecurityException
import java.security.KeyPair
import java.security.KeyPairGenerator
import javax.crypto.KeyAgreement

internal class Curve25519Exchange : KeyExchange {
    private val par: KeyPair = KeyPairGenerator.getInstance("X25519").generateKeyPair()

    override val clavePublicaCliente: ByteArray = RawKeyCodec.crudoX25519(par.public)

    override fun calcularSecreto(clavePublicaServidor: ByteArray): ByteArray {
        val secreto =
            try {
                KeyAgreement.getInstance("X25519").run {
                    init(par.private)
                    doPhase(RawKeyCodec.clavePublicaX25519(clavePublicaServidor), true)
                    generateSecret()
                }
            } catch (error: GeneralSecurityException) {
                throw SshProtocolException("Clave X25519 del servidor inválida", error)
            }
        KeyExchange.exigirSecretoNoNulo(secreto)
        return secreto
    }
}
