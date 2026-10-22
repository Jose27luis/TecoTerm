package pe.amantix.tecoterm.core.ssh.transport

import pe.amantix.tecoterm.core.ssh.SshProtocolException
import pe.amantix.tecoterm.core.ssh.crypto.AlgorithmCategory
import pe.amantix.tecoterm.core.ssh.crypto.SshAlgorithm

internal class AlgorithmNegotiator(
    private val disponibles: (AlgorithmCategory) -> List<SshAlgorithm>,
) {
    fun propuesta(primerIntercambio: Boolean): KexLists {
        val intercambio = nombres(AlgorithmCategory.INTERCAMBIO_CLAVES)
        val cifrado = nombres(AlgorithmCategory.CIFRADO)
        val mac = nombres(AlgorithmCategory.MAC)
        return KexLists(
            intercambio = if (primerIntercambio) intercambio + EXT_INFO_CLIENTE + KEX_ESTRICTO_CLIENTE else intercambio,
            claveHost = nombres(AlgorithmCategory.CLAVE_HOST),
            cifradoClienteServidor = cifrado,
            cifradoServidorCliente = cifrado,
            macClienteServidor = mac,
            macServidorCliente = mac,
            compresionClienteServidor = listOf(SIN_COMPRESION),
            compresionServidorCliente = listOf(SIN_COMPRESION),
        )
    }

    fun negociar(
        cliente: KexLists,
        servidor: KexLists,
        primerIntercambio: Boolean,
    ): NegotiatedAlgorithms {
        elegirNombre("compresión cliente a servidor", cliente.compresionClienteServidor, servidor.compresionClienteServidor)
        elegirNombre("compresión servidor a cliente", cliente.compresionServidorCliente, servidor.compresionServidorCliente)
        return NegotiatedAlgorithms(
            intercambio = elegir("intercambio de claves", cliente.intercambio, servidor.intercambio),
            claveHost = elegir("clave del servidor", cliente.claveHost, servidor.claveHost),
            clienteServidor =
                direccion(
                    elegir("cifrado cliente a servidor", cliente.cifradoClienteServidor, servidor.cifradoClienteServidor),
                    cliente.macClienteServidor,
                    servidor.macClienteServidor,
                ),
            servidorCliente =
                direccion(
                    elegir("cifrado servidor a cliente", cliente.cifradoServidorCliente, servidor.cifradoServidorCliente),
                    cliente.macServidorCliente,
                    servidor.macServidorCliente,
                ),
            kexEstricto = primerIntercambio && KEX_ESTRICTO_SERVIDOR in servidor.intercambio,
        )
    }

    private fun direccion(
        cifrado: SshAlgorithm,
        macCliente: List<String>,
        macServidor: List<String>,
    ): DirectionAlgorithms =
        DirectionAlgorithms(
            cifrado = cifrado,
            mac = if (esAead(cifrado)) null else elegir("integridad", macCliente, macServidor),
        )

    private fun elegir(
        categoria: String,
        cliente: List<String>,
        servidor: List<String>,
    ): SshAlgorithm {
        val nombre = elegirNombre(categoria, cliente, servidor)
        return SshAlgorithm.entries.firstOrNull { it.nombre == nombre }
            ?: throw SshProtocolException("Algoritmo negociado desconocido: $nombre")
    }

    private fun elegirNombre(
        categoria: String,
        cliente: List<String>,
        servidor: List<String>,
    ): String =
        cliente.firstOrNull { it in servidor }
            ?: throw SshProtocolException("No hay un algoritmo común de $categoria con el servidor")

    private fun nombres(categoria: AlgorithmCategory): List<String> = disponibles(categoria).map(SshAlgorithm::nombre)

    companion object {
        const val EXT_INFO_CLIENTE: String = "ext-info-c"
        const val KEX_ESTRICTO_CLIENTE: String = "kex-strict-c-v00@openssh.com"
        const val KEX_ESTRICTO_SERVIDOR: String = "kex-strict-s-v00@openssh.com"
        const val SIN_COMPRESION: String = "none"

        fun esAead(cifrado: SshAlgorithm): Boolean = cifrado == SshAlgorithm.AES256_GCM || cifrado == SshAlgorithm.AES128_GCM
    }
}
