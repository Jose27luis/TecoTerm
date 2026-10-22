package pe.amantix.tecoterm.core.ssh.integration

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import pe.amantix.tecoterm.core.ssh.auth.AuthenticationPrompter
import pe.amantix.tecoterm.core.ssh.auth.Credential
import pe.amantix.tecoterm.core.ssh.auth.KeyboardInteractiveChallenge
import pe.amantix.tecoterm.core.ssh.client.SshClient
import pe.amantix.tecoterm.core.ssh.client.SshConfig
import pe.amantix.tecoterm.core.ssh.client.SshConnection
import pe.amantix.tecoterm.core.ssh.connection.ChannelReader
import pe.amantix.tecoterm.core.ssh.connection.ExitStatus
import pe.amantix.tecoterm.core.ssh.connection.SessionChannel
import pe.amantix.tecoterm.core.ssh.crypto.ClientKey
import pe.amantix.tecoterm.core.ssh.crypto.HostKey
import pe.amantix.tecoterm.core.ssh.transport.HostKeyVerifier
import java.io.ByteArrayOutputStream
import java.util.Base64

internal object TestServer {
    val host: String = variable("TECOTERM_SSHD_HOST")
    val puerto: Int = variable("TECOTERM_SSHD_PORT").toInt()
    val usuario: String = variable("TECOTERM_SSHD_USER")
    private val contrasena: String = variable("TECOTERM_SSHD_PASSWORD")

    val clavesHost: Map<String, HostKey> =
        mapOf(
            "ssh-ed25519" to claveHost("ED25519"),
            "ssh-rsa" to claveHost("RSA"),
            "ecdsa-sha2-nistp256" to claveHost("ECDSA"),
        )

    val verificadorConfiable: HostKeyVerifier = HostKeyVerifier { _, _, clave -> clave in clavesHost.values }

    val sinPreguntas: AuthenticationPrompter =
        object : AuthenticationPrompter {
            override suspend fun responder(desafio: KeyboardInteractiveChallenge): List<CharArray> =
                throw AssertionError("No se esperaba una pregunta: ${desafio.preguntas}")

            override fun mostrarBanner(mensaje: String) = Unit
        }

    val respondeContrasena: AuthenticationPrompter =
        object : AuthenticationPrompter {
            override suspend fun responder(desafio: KeyboardInteractiveChallenge): List<CharArray> =
                desafio.preguntas.map { contrasena.toCharArray() }

            override fun mostrarBanner(mensaje: String) = Unit
        }

    fun contrasena(): Credential.Password = Credential.Password(contrasena.toCharArray())

    fun config(): SshConfig = SshConfig(host = host, usuario = usuario, puerto = puerto)

    suspend fun conectar(
        config: SshConfig = config(),
        credenciales: List<Credential> = listOf(contrasena()),
        verificador: HostKeyVerifier = verificadorConfiable,
        prompter: AuthenticationPrompter = sinPreguntas,
    ): SshConnection = SshClient.conectar(config, verificador, credenciales, prompter)

    suspend fun ejecutar(
        conexion: SshConnection,
        comando: String,
    ): CommandResult = esperar(conexion.ejecutar(comando))

    suspend fun esperar(canal: SessionChannel): CommandResult =
        coroutineScope {
            val salida = async { leerTodo(canal.salida) }
            val errores = async { leerTodo(canal.errores) }
            CommandResult(salida.await(), errores.await(), canal.estadoSalida.await())
        }

    suspend fun leerTodo(lector: ChannelReader): ByteArray {
        val acumulado = ByteArrayOutputStream()
        while (true) {
            val bloque = lector.leer() ?: return acumulado.toByteArray()
            acumulado.write(bloque)
        }
    }

    suspend fun registrarClave(clave: ClientKey) {
        val conexion = conectar()
        val linea = "${clave.tipo} ${Base64.getEncoder().encodeToString(clave.blobPublico)}"
        val resultado = ejecutar(conexion, "echo '$linea' >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys")
        conexion.desconectar()
        check(resultado.estado == ExitStatus.Codigo(0)) { "No se pudo registrar la clave: ${resultado.erroresTexto}" }
    }

    private fun claveHost(tipo: String): HostKey {
        val linea = variable("TECOTERM_SSHD_HOST_KEY_$tipo")
        return HostKey.desdeBlob(Base64.getDecoder().decode(linea.split(' ')[1]))
    }

    private fun variable(nombre: String): String =
        System.getenv(nombre) ?: error("Falta $nombre: las pruebas de integración se ejecutan con infra/test.sh")
}

internal class CommandResult(
    val salida: ByteArray,
    val errores: ByteArray,
    val estado: ExitStatus,
) {
    val salidaTexto: String
        get() = String(salida, Charsets.UTF_8)

    val erroresTexto: String
        get() = String(errores, Charsets.UTF_8)
}
