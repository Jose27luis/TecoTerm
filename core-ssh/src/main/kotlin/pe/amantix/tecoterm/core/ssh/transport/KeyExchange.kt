package pe.amantix.tecoterm.core.ssh.transport

import pe.amantix.tecoterm.core.ssh.SshProtocolException
import pe.amantix.tecoterm.core.ssh.crypto.SshAlgorithm

internal interface KeyExchange {
    val clavePublicaCliente: ByteArray

    fun calcularSecreto(clavePublicaServidor: ByteArray): ByteArray

    companion object {
        fun crear(algoritmo: SshAlgorithm): KeyExchange =
            when (algoritmo) {
                SshAlgorithm.CURVE25519_SHA256, SshAlgorithm.CURVE25519_SHA256_LIBSSH -> Curve25519Exchange()
                SshAlgorithm.ECDH_SHA2_NISTP256 -> EcdhP256Exchange()
                else -> throw SshProtocolException("${algoritmo.nombre} no es un intercambio de claves")
            }

        fun exigirSecretoNoNulo(secreto: ByteArray) {
            var acumulado = 0
            secreto.forEach { acumulado = acumulado or it.toInt() }
            if (acumulado == 0) {
                throw SshProtocolException("El intercambio de claves produjo un secreto nulo")
            }
        }
    }
}
