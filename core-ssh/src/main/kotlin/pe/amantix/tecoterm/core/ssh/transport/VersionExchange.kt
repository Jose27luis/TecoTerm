package pe.amantix.tecoterm.core.ssh.transport

import pe.amantix.tecoterm.core.ssh.ConnectionLostException
import pe.amantix.tecoterm.core.ssh.SshProtocolException
import java.io.InputStream
import java.io.OutputStream

internal object VersionExchange {
    const val VERSION_CLIENTE: String = "SSH-2.0-TecoTerm_0.1.0"
    private const val LONGITUD_MAXIMA_LINEA: Int = 255
    private const val MAXIMO_LINEAS_PREVIAS: Int = 64
    private const val FIN_DE_FLUJO: Int = -1
    private const val SALTO: Int = '\n'.code
    private const val RETORNO: Int = '\r'.code
    private val prefijosAceptados: List<String> = listOf("SSH-2.0-", "SSH-1.99-")

    fun enviar(salida: OutputStream) {
        salida.write("$VERSION_CLIENTE\r\n".toByteArray(Charsets.US_ASCII))
        salida.flush()
    }

    fun leer(entrada: InputStream): String {
        repeat(MAXIMO_LINEAS_PREVIAS) {
            val linea = leerLinea(entrada)
            if (linea.startsWith("SSH-")) {
                if (prefijosAceptados.none { linea.startsWith(it) }) {
                    throw SshProtocolException("Versión de protocolo no soportada: $linea")
                }
                return linea
            }
        }
        throw SshProtocolException("El servidor no envió su versión de protocolo")
    }

    private fun leerLinea(entrada: InputStream): String {
        val bytes = ByteArray(LONGITUD_MAXIMA_LINEA)
        var longitud = 0
        while (true) {
            val leido = entrada.read()
            when {
                leido == FIN_DE_FLUJO -> {
                    throw ConnectionLostException("El servidor cerró la conexión antes de enviar su versión")
                }

                leido == SALTO -> {
                    val fin = if (longitud > 0 && bytes[longitud - 1].toInt() == RETORNO) longitud - 1 else longitud
                    return String(bytes, 0, fin, Charsets.US_ASCII)
                }

                leido == 0 || longitud == LONGITUD_MAXIMA_LINEA -> {
                    throw SshProtocolException("Línea de versión inválida")
                }

                else -> {
                    bytes[longitud++] = leido.toByte()
                }
            }
        }
    }
}
