package pe.amantix.tecoterm.core.ssh.transport

internal data class KexLists(
    val intercambio: List<String>,
    val claveHost: List<String>,
    val cifradoClienteServidor: List<String>,
    val cifradoServidorCliente: List<String>,
    val macClienteServidor: List<String>,
    val macServidorCliente: List<String>,
    val compresionClienteServidor: List<String>,
    val compresionServidorCliente: List<String>,
)
