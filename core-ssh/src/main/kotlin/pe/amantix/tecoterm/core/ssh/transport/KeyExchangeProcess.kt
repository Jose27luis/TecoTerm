package pe.amantix.tecoterm.core.ssh.transport

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.amantix.tecoterm.core.ssh.HostKeyRejectedException
import pe.amantix.tecoterm.core.ssh.SshProtocolException
import pe.amantix.tecoterm.core.ssh.crypto.HostKey
import pe.amantix.tecoterm.core.ssh.transport.cipher.DerivationLetters
import pe.amantix.tecoterm.core.ssh.transport.cipher.PacketCipherFactory
import pe.amantix.tecoterm.core.ssh.wire.SshMessage
import pe.amantix.tecoterm.core.ssh.wire.SshReader
import pe.amantix.tecoterm.core.ssh.wire.SshWriter

internal class KeyExchangeProcess(
    private val canal: PacketChannel,
    private val escritura: Mutex,
    private val negociador: AlgorithmNegotiator,
    private val fabrica: PacketCipherFactory,
    private val opciones: TransportOptions,
    private val versionServidor: String,
) {
    suspend fun ejecutar(
        propio: KexInit,
        paqueteServidor: IncomingPacket,
        identidad: SessionIdentity?,
    ): SessionIdentity {
        val kexServidor = KexInit.leer(paqueteServidor.payload)
        val algoritmos = negociador.negociar(propio.listas, kexServidor.listas, primerIntercambio = identidad == null)
        val estricto = identidad?.kexEstricto ?: algoritmos.kexEstricto
        exigirOrdenEstricto(identidad == null && estricto, paqueteServidor)
        if (kexServidor.primerPaqueteSigue && !adivinoElServidor(kexServidor.listas, algoritmos)) {
            canal.leer()
        }
        val intercambio = KeyExchange.crear(algoritmos.intercambio)
        val respuesta = intercambiarPublicas(intercambio, algoritmos, estricto)
        val secreto = intercambio.calcularSecreto(respuesta.publicaServidor)
        try {
            val hash =
                ExchangeHash(VersionExchange.VERSION_CLIENTE, versionServidor, propio.payload, kexServidor.payload)
                    .calcular(respuesta.blobClaveHost, intercambio.clavePublicaCliente, respuesta.publicaServidor, secreto)
            if (!respuesta.claveHost.verificar(algoritmos.claveHost.nombre, hash, respuesta.firma)) {
                throw SshProtocolException("La firma del intercambio de claves no es válida")
            }
            val nuevaIdentidad = confirmarIdentidad(identidad, respuesta.claveHost, hash, estricto)
            activarCifrados(algoritmos, KeyDerivation(secreto, hash, nuevaIdentidad.idSesion), estricto)
            return nuevaIdentidad
        } finally {
            secreto.fill(0)
        }
    }

    private fun exigirOrdenEstricto(
        aplica: Boolean,
        paqueteServidor: IncomingPacket,
    ) {
        if (aplica && paqueteServidor.secuencia != 0) {
            throw SshProtocolException("El KEXINIT del servidor no fue su primer mensaje")
        }
    }

    private suspend fun intercambiarPublicas(
        intercambio: KeyExchange,
        algoritmos: NegotiatedAlgorithms,
        estricto: Boolean,
    ): EcdhReply {
        escritura.withLock {
            canal.escribir(
                SshWriter()
                    .escribirByte(SshMessage.SSH_MSG_KEX_ECDH_INIT)
                    .escribirString(intercambio.clavePublicaCliente)
                    .bytes(),
            )
        }
        val lector = SshReader(leerIntercambio(SshMessage.SSH_MSG_KEX_ECDH_REPLY, estricto))
        lector.leerByte()
        val blobClaveHost = lector.leerString()
        val publicaServidor = lector.leerString()
        val firma = lector.leerString()
        lector.verificarFin()
        val claveHost = HostKey.desdeBlob(blobClaveHost)
        if (!claveHost.admite(algoritmos.claveHost.nombre)) {
            throw SshProtocolException("El servidor envió una clave ${claveHost.tipo} para ${algoritmos.claveHost.nombre}")
        }
        return EcdhReply(blobClaveHost, claveHost, publicaServidor, firma)
    }

    fun leerIntercambio(
        esperado: Int,
        estricto: Boolean,
    ): ByteArray {
        while (true) {
            val paquete = canal.leer()
            when {
                paquete.tipo == esperado -> return paquete.payload
                paquete.tipo == SshMessage.SSH_MSG_DISCONNECT -> throw TransportMessages.desconexion(paquete.payload)
                !estricto && (paquete.tipo == SshMessage.SSH_MSG_IGNORE || paquete.tipo == SshMessage.SSH_MSG_DEBUG) -> Unit
                else -> throw SshProtocolException("Mensaje ${paquete.tipo} inesperado durante el intercambio de claves")
            }
        }
    }

    private suspend fun confirmarIdentidad(
        identidad: SessionIdentity?,
        claveHost: HostKey,
        hash: ByteArray,
        estricto: Boolean,
    ): SessionIdentity {
        if (identidad != null) {
            if (claveHost != identidad.claveHost) {
                throw SshProtocolException("La clave del servidor cambió durante el re-key")
            }
            return identidad
        }
        if (!opciones.verificador.verificar(opciones.host, opciones.puerto, claveHost)) {
            throw HostKeyRejectedException(claveHost.huellaSha256())
        }
        return SessionIdentity(hash, claveHost, estricto)
    }

    private suspend fun activarCifrados(
        algoritmos: NegotiatedAlgorithms,
        derivacion: KeyDerivation,
        estricto: Boolean,
    ) {
        try {
            val salida = fabrica.crear(algoritmos.clienteServidor, derivacion, DerivationLetters.clienteServidor)
            val entrada = fabrica.crear(algoritmos.servidorCliente, derivacion, DerivationLetters.servidorCliente)
            escritura.withLock {
                canal.escribir(byteArrayOf(SshMessage.SSH_MSG_NEWKEYS.toByte()))
                canal.activarCifradoSalida(salida, reiniciarSecuencia = estricto)
            }
            val nuevasClaves = leerIntercambio(SshMessage.SSH_MSG_NEWKEYS, estricto)
            if (nuevasClaves.size != 1) {
                throw SshProtocolException("SSH_MSG_NEWKEYS con datos de más")
            }
            canal.activarCifradoEntrada(entrada, reiniciarSecuencia = estricto)
        } finally {
            derivacion.destruir()
        }
    }

    private fun adivinoElServidor(
        servidor: KexLists,
        algoritmos: NegotiatedAlgorithms,
    ): Boolean =
        servidor.intercambio.firstOrNull() == algoritmos.intercambio.nombre &&
            servidor.claveHost.firstOrNull() == algoritmos.claveHost.nombre

    private class EcdhReply(
        val blobClaveHost: ByteArray,
        val claveHost: HostKey,
        val publicaServidor: ByteArray,
        val firma: ByteArray,
    )
}
