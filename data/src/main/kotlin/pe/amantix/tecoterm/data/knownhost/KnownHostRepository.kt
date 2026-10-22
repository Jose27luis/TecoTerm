package pe.amantix.tecoterm.data.knownhost

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.amantix.tecoterm.core.ssh.transport.HostKeyVerifier

public class KnownHostRepository internal constructor(
    private val dao: KnownHostDao,
    private val reloj: () -> Long,
) {
    public val servidores: Flow<List<KnownHost>> =
        dao.observar().map { lista ->
            lista.map { KnownHost(it.host, it.puerto, it.tipoClave, it.huella, it.vistoPorPrimeraVez) }
        }

    public suspend fun tipoConocido(
        host: String,
        puerto: Int,
    ): String? = dao.obtener(host, puerto)?.tipoClave

    public suspend fun olvidar(
        host: String,
        puerto: Int,
    ) {
        dao.eliminar(host, puerto)
    }

    public fun verificador(prompter: HostKeyPrompter): HostKeyVerifier = KnownHostVerifier(dao, prompter, reloj)
}
