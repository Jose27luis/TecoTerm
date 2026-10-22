package pe.amantix.tecoterm.core.ssh.transport.cipher

import pe.amantix.tecoterm.core.ssh.SshProtocolException
import java.io.DataInputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class AesCtrPacketCipher(
    clave: ByteArray,
    iv: ByteArray,
    private val mac: HmacSha2,
    private val cifrarAntesDeMac: Boolean,
    private val aleatorio: SecureRandom,
) : PacketCipher {
    private val flujo: Cipher =
        Cipher.getInstance("AES/CTR/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(clave, "AES"), IvParameterSpec(iv))
        }

    override fun empaquetar(
        payload: ByteArray,
        secuencia: Int,
    ): ByteArray {
        val plano = PacketLayout.construir(payload, BLOQUE, alineacionIncluyeLongitud = !cifrarAntesDeMac, aleatorio = aleatorio)
        val resultado =
            if (cifrarAntesDeMac) {
                val paquete = plano.copyOf(PacketLayout.BYTES_LONGITUD) + transformar(plano, PacketLayout.BYTES_LONGITUD)
                paquete + mac.calcular(secuencia, paquete)
            } else {
                val etiqueta = mac.calcular(secuencia, plano)
                transformar(plano, 0) + etiqueta
            }
        plano.fill(0)
        return resultado
    }

    override fun leer(
        entrada: DataInputStream,
        secuencia: Int,
    ): ByteArray = if (cifrarAntesDeMac) leerConMacSobreCifrado(entrada, secuencia) else leerConMacSobrePlano(entrada, secuencia)

    private fun leerConMacSobreCifrado(
        entrada: DataInputStream,
        secuencia: Int,
    ): ByteArray {
        val cabecera = ByteArray(PacketLayout.BYTES_LONGITUD)
        entrada.readFully(cabecera)
        val longitud = PacketLayout.leerLongitud(cabecera, BLOQUE, alineacionIncluyeLongitud = false)
        val cifrado = ByteArray(longitud)
        entrada.readFully(cifrado)
        val etiqueta = ByteArray(mac.longitud)
        entrada.readFully(etiqueta)
        if (!mac.coincide(secuencia, cabecera + cifrado, etiqueta)) {
            throw SshProtocolException("El código de integridad del paquete no coincide")
        }
        val plano = transformar(cifrado, 0)
        val payload = PacketLayout.extraerPayload(plano, 0)
        plano.fill(0)
        return payload
    }

    private fun leerConMacSobrePlano(
        entrada: DataInputStream,
        secuencia: Int,
    ): ByteArray {
        val primerBloque = ByteArray(BLOQUE)
        entrada.readFully(primerBloque)
        val inicio = transformar(primerBloque, 0)
        val longitud = PacketLayout.leerLongitud(inicio, BLOQUE, alineacionIncluyeLongitud = true)
        val resto = ByteArray(PacketLayout.BYTES_LONGITUD + longitud - BLOQUE)
        entrada.readFully(resto)
        val plano = inicio + transformar(resto, 0)
        val etiqueta = ByteArray(mac.longitud)
        entrada.readFully(etiqueta)
        if (!mac.coincide(secuencia, plano, etiqueta)) {
            throw SshProtocolException("El código de integridad del paquete no coincide")
        }
        val payload = PacketLayout.extraerPayload(plano, PacketLayout.BYTES_LONGITUD)
        plano.fill(0)
        inicio.fill(0)
        return payload
    }

    private fun transformar(
        datos: ByteArray,
        desde: Int,
    ): ByteArray {
        val longitud = datos.size - desde
        val salida = flujo.update(datos, desde, longitud) ?: ByteArray(0)
        if (salida.size != longitud) {
            throw SshProtocolException("El cifrado de flujo devolvió una longitud inesperada")
        }
        return salida
    }

    private companion object {
        const val BLOQUE: Int = 16
    }
}
