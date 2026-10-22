package pe.amantix.tecoterm.core.ssh.client

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import pe.amantix.tecoterm.core.ssh.ConnectionLostException
import pe.amantix.tecoterm.core.ssh.ConnectionRefusedException
import pe.amantix.tecoterm.core.ssh.ConnectionTimeoutException
import pe.amantix.tecoterm.core.ssh.HostNotFoundException
import pe.amantix.tecoterm.core.ssh.SshException
import pe.amantix.tecoterm.core.ssh.SshProtocolException
import pe.amantix.tecoterm.core.ssh.auth.AuthenticationPrompter
import pe.amantix.tecoterm.core.ssh.auth.Credential
import pe.amantix.tecoterm.core.ssh.auth.UserAuthentication
import pe.amantix.tecoterm.core.ssh.crypto.AlgorithmCategory
import pe.amantix.tecoterm.core.ssh.crypto.PlatformAlgorithms
import pe.amantix.tecoterm.core.ssh.transport.HostKeyVerifier
import pe.amantix.tecoterm.core.ssh.transport.PacketChannel
import pe.amantix.tecoterm.core.ssh.transport.Transport
import pe.amantix.tecoterm.core.ssh.transport.TransportOptions
import pe.amantix.tecoterm.core.ssh.transport.VersionExchange
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.IOException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.security.SecureRandom

public object SshClient {
    public suspend fun conectar(
        config: SshConfig,
        verificador: HostKeyVerifier,
        credenciales: List<Credential>,
        prompter: AuthenticationPrompter,
    ): SshConnection =
        withContext(Dispatchers.IO) {
            val socket = abrirSocket(config)
            val cancelacion =
                currentCoroutineContext()[Job]?.invokeOnCompletion { causa ->
                    if (causa is CancellationException) {
                        cerrarSocket(socket)
                    }
                }
            var transport: Transport? = null
            val alcance =
                CoroutineScope(
                    SupervisorJob() +
                        Dispatchers.IO +
                        CoroutineExceptionHandler { _, error ->
                            transport?.cerrar(SshProtocolException("Error interno del cliente SSH", error))
                        },
                )
            try {
                socket.soTimeout = config.tiempoIntercambio.inWholeMilliseconds.toInt()
                val entrada = DataInputStream(BufferedInputStream(socket.getInputStream()))
                val salida = BufferedOutputStream(socket.getOutputStream())
                val versionServidor =
                    try {
                        VersionExchange.enviar(salida)
                        VersionExchange.leer(entrada)
                    } catch (error: SocketTimeoutException) {
                        throw ConnectionTimeoutException("El servidor no envió su versión a tiempo", error)
                    } catch (error: IOException) {
                        throw ConnectionLostException("Se perdió la conexión al intercambiar versiones", error)
                    }
                val aleatorio = SecureRandom()
                val nuevo =
                    Transport(
                        PacketChannel(entrada, salida, aleatorio),
                        versionServidor,
                        opciones(config, verificador),
                        alcance,
                        aleatorio,
                    ) {
                        cerrarSocket(socket)
                    }
                transport = nuevo
                nuevo.iniciar()
                socket.soTimeout = 0
                UserAuthentication(nuevo, config.usuario, prompter).autenticar(credenciales)
                SshConnection(nuevo, alcance, config)
            } catch (error: SshException) {
                transport?.cerrar(error)
                alcance.cancel()
                cerrarSocket(socket)
                throw error
            } finally {
                cancelacion?.dispose()
            }
        }

    private fun abrirSocket(config: SshConfig): Socket {
        val direccion = InetSocketAddress(config.host, config.puerto)
        if (direccion.isUnresolved) {
            throw HostNotFoundException(config.host, IOException("No se pudo resolver ${config.host}"))
        }
        val socket = Socket()
        try {
            socket.connect(direccion, config.tiempoConexion.inWholeMilliseconds.toInt())
            socket.tcpNoDelay = true
            socket.keepAlive = true
            return socket
        } catch (error: IOException) {
            cerrarSocket(socket)
            throw traducirConexion(error, config)
        }
    }

    private fun traducirConexion(
        error: IOException,
        config: SshConfig,
    ): SshException =
        when (error) {
            is ConnectException -> {
                ConnectionRefusedException(config.host, config.puerto, error)
            }

            is SocketTimeoutException -> {
                ConnectionTimeoutException("El servidor ${config.host}:${config.puerto} no respondió a tiempo", error)
            }

            else -> {
                ConnectionLostException("No se pudo conectar con ${config.host}:${config.puerto}", error)
            }
        }

    private fun opciones(
        config: SshConfig,
        verificador: HostKeyVerifier,
    ): TransportOptions =
        TransportOptions(
            host = config.host,
            puerto = config.puerto,
            verificador = verificador,
            algoritmos = { categoria: AlgorithmCategory ->
                PlatformAlgorithms.disponibles(categoria).filter { it in config.algoritmosPermitidos }
            },
            limiteBytesRekey = config.limiteBytesRekey,
            limiteTiempoRekey = config.limiteTiempoRekey,
        )

    private fun cerrarSocket(socket: Socket) {
        try {
            socket.close()
        } catch (_: IOException) {
        }
    }
}
