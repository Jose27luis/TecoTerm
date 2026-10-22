package pe.amantix.tecoterm.core.ssh.transport

import pe.amantix.tecoterm.core.ssh.ConnectionLostException
import pe.amantix.tecoterm.core.ssh.ConnectionTimeoutException
import pe.amantix.tecoterm.core.ssh.SshException
import pe.amantix.tecoterm.core.ssh.transport.cipher.PacketCipher
import pe.amantix.tecoterm.core.ssh.transport.cipher.PlainPacketCipher
import java.io.DataInputStream
import java.io.EOFException
import java.io.IOException
import java.io.OutputStream
import java.net.SocketTimeoutException
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicLong

internal class PacketChannel(
    private val entrada: DataInputStream,
    private val salida: OutputStream,
    aleatorio: SecureRandom,
) {
    @Volatile
    private var cifradoEntrada: PacketCipher = PlainPacketCipher(aleatorio)

    @Volatile
    private var cifradoSalida: PacketCipher = PlainPacketCipher(aleatorio)

    private var secuenciaEntrada: Int = 0
    private var secuenciaSalida: Int = 0
    private val bytesTransferidos: AtomicLong = AtomicLong(0)

    val bytesDesdeIntercambio: Long
        get() = bytesTransferidos.get()

    fun leer(): IncomingPacket {
        val payload =
            try {
                cifradoEntrada.leer(entrada, secuenciaEntrada)
            } catch (error: IOException) {
                throw traducirLectura(error)
            }
        val paquete = IncomingPacket(secuenciaEntrada, payload)
        secuenciaEntrada++
        bytesTransferidos.addAndGet(payload.size.toLong())
        return paquete
    }

    fun escribir(payload: ByteArray) {
        val paquete = cifradoSalida.empaquetar(payload, secuenciaSalida)
        secuenciaSalida++
        try {
            salida.write(paquete)
            salida.flush()
        } catch (error: IOException) {
            throw ConnectionLostException("No se pudo enviar al servidor", error)
        }
        bytesTransferidos.addAndGet(payload.size.toLong())
    }

    fun activarCifradoSalida(
        cifrado: PacketCipher,
        reiniciarSecuencia: Boolean,
    ) {
        cifradoSalida = cifrado
        if (reiniciarSecuencia) {
            secuenciaSalida = 0
        }
    }

    fun activarCifradoEntrada(
        cifrado: PacketCipher,
        reiniciarSecuencia: Boolean,
    ) {
        cifradoEntrada = cifrado
        if (reiniciarSecuencia) {
            secuenciaEntrada = 0
        }
    }

    fun reiniciarContador() {
        bytesTransferidos.set(0)
    }

    private fun traducirLectura(error: IOException): SshException =
        when (error) {
            is EOFException -> ConnectionLostException("El servidor cerró la conexión", error)
            is SocketTimeoutException -> ConnectionTimeoutException("El servidor no respondió a tiempo", error)
            else -> ConnectionLostException("Se perdió la conexión con el servidor", error)
        }
}
