package pe.amantix.tecoterm.data.knownhost

import pe.amantix.tecoterm.core.ssh.crypto.HostKey
import pe.amantix.tecoterm.core.ssh.transport.HostKeyVerifier
import java.security.MessageDigest

internal class KnownHostVerifier(
    private val dao: KnownHostDao,
    private val prompter: HostKeyPrompter,
    private val reloj: () -> Long,
) : HostKeyVerifier {
    override suspend fun verificar(
        host: String,
        puerto: Int,
        clave: HostKey,
    ): Boolean {
        val conocido = dao.obtener(host, puerto)
        val blob = clave.blob
        return when {
            conocido == null -> {
                registrarSiAcepta(host, puerto, clave)
            }

            MessageDigest.isEqual(conocido.clave, blob) -> {
                true
            }

            else -> {
                prompter.avisarCambio(
                    HostKeyChange(
                        host = host,
                        puerto = puerto,
                        tipoAnterior = conocido.tipoClave,
                        huellaAnterior = conocido.huella,
                        tipoNuevo = clave.tipo,
                        huellaNueva = clave.huellaSha256(),
                    ),
                )
                false
            }
        }
    }

    private suspend fun registrarSiAcepta(
        host: String,
        puerto: Int,
        clave: HostKey,
    ): Boolean {
        val huella = clave.huellaSha256()
        val acepta = prompter.confirmarNuevo(HostKeyPrompt(host, puerto, clave.tipo, huella))
        if (acepta) {
            dao.guardar(KnownHostEntity(host, puerto, clave.tipo, clave.blob, huella, reloj()))
        }
        return acepta
    }
}
