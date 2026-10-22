package pe.amantix.tecoterm.core.ssh.keys

import pe.amantix.tecoterm.core.ssh.InvalidKeyFileException
import java.util.Base64

internal class PemBlock(
    val tipo: String,
    val cabeceras: Map<String, String>,
    val contenido: ByteArray,
)

internal object PemArmor {
    private val inicio: Regex = Regex("^-----BEGIN ([A-Z0-9 ]+)-----$")
    private const val SEPARADOR_CABECERA: Char = ':'

    fun leer(texto: String): PemBlock {
        val lineas = texto.lines().map(String::trim).filter(String::isNotEmpty)
        val indiceInicio = lineas.indexOfFirst { inicio.matches(it) }
        if (indiceInicio < 0) {
            throw InvalidKeyFileException("El archivo no contiene una clave privada en formato PEM u OpenSSH")
        }
        val tipo =
            inicio
                .find(lineas[indiceInicio])
                ?.groupValues
                ?.get(1)
                .orEmpty()
        val indiceFin = lineas.indexOf("-----END $tipo-----")
        if (indiceFin < indiceInicio) {
            throw InvalidKeyFileException("Falta la línea final de la clave")
        }
        val cuerpo = lineas.subList(indiceInicio + 1, indiceFin)
        val cabeceras =
            cuerpo
                .takeWhile { SEPARADOR_CABECERA in it }
                .associate { it.substringBefore(SEPARADOR_CABECERA).trim() to it.substringAfter(SEPARADOR_CABECERA).trim() }
        return PemBlock(tipo, cabeceras, decodificar(cuerpo.drop(cabeceras.size).joinToString("")))
    }

    private fun decodificar(base64: String): ByteArray =
        try {
            Base64.getDecoder().decode(base64)
        } catch (error: IllegalArgumentException) {
            throw InvalidKeyFileException("El contenido de la clave no es Base64 válido", error)
        }
}
