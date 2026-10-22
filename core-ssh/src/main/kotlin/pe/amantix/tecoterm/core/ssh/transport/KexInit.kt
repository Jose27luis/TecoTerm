package pe.amantix.tecoterm.core.ssh.transport

import pe.amantix.tecoterm.core.ssh.SshProtocolException
import pe.amantix.tecoterm.core.ssh.wire.SshMessage
import pe.amantix.tecoterm.core.ssh.wire.SshReader
import pe.amantix.tecoterm.core.ssh.wire.SshWriter

internal class KexInit(
    val payload: ByteArray,
    val listas: KexLists,
    val primerPaqueteSigue: Boolean,
) {
    companion object {
        const val LONGITUD_COOKIE: Int = 16

        fun crear(
            cookie: ByteArray,
            listas: KexLists,
        ): KexInit {
            require(cookie.size == LONGITUD_COOKIE) { "La cookie de KEXINIT debe tener $LONGITUD_COOKIE bytes" }
            val escritor =
                SshWriter()
                    .escribirByte(SshMessage.SSH_MSG_KEXINIT)
                    .escribirBytes(cookie)
                    .escribirNameList(listas.intercambio)
                    .escribirNameList(listas.claveHost)
                    .escribirNameList(listas.cifradoClienteServidor)
                    .escribirNameList(listas.cifradoServidorCliente)
                    .escribirNameList(listas.macClienteServidor)
                    .escribirNameList(listas.macServidorCliente)
                    .escribirNameList(listas.compresionClienteServidor)
                    .escribirNameList(listas.compresionServidorCliente)
                    .escribirNameList(emptyList())
                    .escribirNameList(emptyList())
                    .escribirBoolean(false)
                    .escribirUint32(0)
            return KexInit(escritor.bytes(), listas, primerPaqueteSigue = false)
        }

        fun leer(payload: ByteArray): KexInit {
            val lector = SshReader(payload)
            if (lector.leerByte() != SshMessage.SSH_MSG_KEXINIT) {
                throw SshProtocolException("Se esperaba SSH_MSG_KEXINIT")
            }
            lector.leerBytes(LONGITUD_COOKIE)
            val listas =
                KexLists(
                    intercambio = lector.leerNameList(),
                    claveHost = lector.leerNameList(),
                    cifradoClienteServidor = lector.leerNameList(),
                    cifradoServidorCliente = lector.leerNameList(),
                    macClienteServidor = lector.leerNameList(),
                    macServidorCliente = lector.leerNameList(),
                    compresionClienteServidor = lector.leerNameList(),
                    compresionServidorCliente = lector.leerNameList(),
                )
            lector.leerNameList()
            lector.leerNameList()
            val primerPaqueteSigue = lector.leerBoolean()
            lector.leerUint32()
            lector.verificarFin()
            return KexInit(payload, listas, primerPaqueteSigue)
        }
    }
}
