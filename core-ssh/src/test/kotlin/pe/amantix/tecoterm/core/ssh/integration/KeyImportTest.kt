package pe.amantix.tecoterm.core.ssh.integration

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import pe.amantix.tecoterm.core.ssh.PassphraseRequiredException
import pe.amantix.tecoterm.core.ssh.WrongPassphraseException
import pe.amantix.tecoterm.core.ssh.auth.Credential
import pe.amantix.tecoterm.core.ssh.client.SshConnection
import pe.amantix.tecoterm.core.ssh.keys.PrivateKeyImporter
import java.util.Base64
import kotlin.time.Duration.Companion.seconds

internal class KeyImportTest {
    @ParameterizedTest
    @MethodSource("casos")
    fun `la clave de ssh-keygen se importa y autentica`(caso: KeyCase) {
        runBlocking {
            withTimeout(120.seconds) {
                val conexion = TestServer.conectar()
                val (privada, publica) = generarEnServidor(conexion, caso)
                conexion.desconectar()

                if (caso.passphrase.isNotEmpty()) {
                    assertThrows<PassphraseRequiredException> { PrivateKeyImporter.importar(privada, null) }
                    assertThrows<WrongPassphraseException> { PrivateKeyImporter.importar(privada, "incorrecta".toCharArray()) }
                }
                val importada = PrivateKeyImporter.importar(privada, caso.passphrase.toCharArray().takeIf { it.isNotEmpty() })

                assertArrayEquals(Base64.getDecoder().decode(publica.split(' ')[1]), importada.clave.blobPublico)
                if (caso.formatoOpenSsh) {
                    assertEquals(COMENTARIO, importada.comentario)
                }
                TestServer.registrarClave(importada.clave)
                val conClave = TestServer.conectar(credenciales = listOf(Credential.PublicKey(importada.clave)))
                assertEquals("${TestServer.usuario}\n", TestServer.ejecutar(conClave, "whoami").salidaTexto)
                conClave.desconectar()
            }
        }
    }

    private suspend fun generarEnServidor(
        conexion: SshConnection,
        caso: KeyCase,
    ): Pair<String, String> {
        val comando =
            "rm -f /tmp/clave /tmp/clave.pub && " +
                "ssh-keygen -q ${caso.opciones} -N '${caso.passphrase}' -C $COMENTARIO -f /tmp/clave && " +
                "cat /tmp/clave && echo $SEPARADOR && cat /tmp/clave.pub"
        val resultado = TestServer.ejecutar(conexion, comando)
        val partes = resultado.salidaTexto.split("$SEPARADOR\n")
        check(partes.size == 2) { "ssh-keygen falló: ${resultado.erroresTexto}" }
        return partes[0] to partes[1].trim()
    }

    internal class KeyCase(
        val opciones: String,
        val passphrase: String,
        val formatoOpenSsh: Boolean,
    ) {
        override fun toString(): String = "$opciones ${if (passphrase.isEmpty()) "sin" else "con"} passphrase"
    }

    companion object {
        private const val COMENTARIO: String = "prueba"
        private const val SEPARADOR: String = "----FIN----"

        @JvmStatic
        fun casos(): List<KeyCase> {
            val formatos =
                listOf(
                    "-t ed25519" to true,
                    "-t ed25519 -Z aes256-gcm@openssh.com" to true,
                    "-t rsa -b 3072" to true,
                    "-t rsa -b 3072 -m PEM" to false,
                    "-t rsa -b 3072 -m PKCS8" to false,
                    "-t ecdsa -b 256" to true,
                    "-t ecdsa -b 256 -m PEM" to false,
                    "-t ecdsa -b 256 -m PKCS8" to false,
                )
            return formatos
                .flatMap { (opciones, openSsh) ->
                    listOf(KeyCase(opciones, "", openSsh), KeyCase(opciones, "frase segura", openSsh))
                }.filterNot { it.opciones.contains("-Z") && it.passphrase.isEmpty() }
        }
    }
}
