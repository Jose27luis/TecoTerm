package pe.amantix.tecoterm.core.ssh.crypto

import pe.amantix.tecoterm.core.ssh.wire.SshReader
import java.security.PublicKey

public class Ed25519HostKey internal constructor(
    blob: ByteArray,
    internal val clave: PublicKey,
) : HostKey(blob) {
    override val tipo: String = TIPO

    override fun admite(algoritmoFirma: String): Boolean = algoritmoFirma == TIPO

    override fun verificarFirma(
        algoritmoFirma: String,
        datos: ByteArray,
        firma: ByteArray,
    ): Boolean = firma.size == LONGITUD_FIRMA && verificarConPlataforma("Ed25519", clave, datos, firma)

    internal companion object {
        const val TIPO: String = "ssh-ed25519"
        private const val LONGITUD_FIRMA: Int = 64

        fun leer(
            blob: ByteArray,
            lector: SshReader,
        ): Ed25519HostKey {
            val crudo = lector.leerString()
            lector.verificarFin()
            return Ed25519HostKey(blob, RawKeyCodec.clavePublicaEd25519(crudo))
        }
    }
}
