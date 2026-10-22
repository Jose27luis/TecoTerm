package pe.amantix.tecoterm.core.ssh.keys

import pe.amantix.tecoterm.core.ssh.crypto.AlgorithmCategory
import pe.amantix.tecoterm.core.ssh.crypto.ClientKey
import pe.amantix.tecoterm.core.ssh.crypto.EcP256
import pe.amantix.tecoterm.core.ssh.crypto.PlatformAlgorithms
import pe.amantix.tecoterm.core.ssh.crypto.PlatformPrimitive
import pe.amantix.tecoterm.core.ssh.crypto.SshAlgorithm
import java.security.KeyPairGenerator

public object ClientKeyGenerator {
    public fun generar(): ClientKey {
        val ed25519Disponible =
            SshAlgorithm.SSH_ED25519 in PlatformAlgorithms.disponibles(AlgorithmCategory.CLAVE_HOST) &&
                PlatformPrimitive.KeyPairGeneratorPrimitive("Ed25519").estaDisponible()
        val par =
            if (ed25519Disponible) {
                KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
            } else {
                KeyPairGenerator.getInstance("EC").run {
                    initialize(EcP256.parametros)
                    generateKeyPair()
                }
            }
        return ClientKey.desde(par)
    }
}
