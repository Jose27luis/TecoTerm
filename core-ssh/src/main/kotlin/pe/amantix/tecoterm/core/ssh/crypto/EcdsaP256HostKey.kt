package pe.amantix.tecoterm.core.ssh.crypto

import pe.amantix.tecoterm.core.ssh.SshException
import pe.amantix.tecoterm.core.ssh.SshProtocolException
import pe.amantix.tecoterm.core.ssh.wire.SshReader
import java.security.PublicKey
import java.security.interfaces.ECPublicKey

public class EcdsaP256HostKey internal constructor(
    blob: ByteArray,
    internal val clave: ECPublicKey,
) : HostKey(blob) {
    override val tipo: String = TIPO

    override val clavePublica: PublicKey = clave

    override val algoritmoPlataforma: String = "EC"

    override fun admite(algoritmoFirma: String): Boolean = algoritmoFirma == TIPO

    override fun verificarFirma(
        algoritmoFirma: String,
        datos: ByteArray,
        firma: ByteArray,
    ): Boolean {
        val der =
            try {
                val lector = SshReader(firma)
                val r = lector.leerMpint()
                val s = lector.leerMpint()
                lector.verificarFin()
                EcdsaSignatureCodec.derDesdeRs(r, s)
            } catch (_: SshException) {
                return false
            }
        return verificarConPlataforma("SHA256withECDSA", clave, datos, der)
    }

    internal companion object {
        const val TIPO: String = "ecdsa-sha2-nistp256"

        fun leer(
            blob: ByteArray,
            lector: SshReader,
        ): EcdsaP256HostKey {
            val curva = String(lector.leerString(), Charsets.US_ASCII)
            val punto = lector.leerString()
            lector.verificarFin()
            if (curva != EcP256.NOMBRE_CURVA) {
                throw SshProtocolException("Curva ECDSA no soportada: $curva")
            }
            return EcdsaP256HostKey(blob, EcP256.clavePublica(EcP256.puntoDesdeBytes(punto)))
        }
    }
}
