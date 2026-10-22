package pe.amantix.tecoterm.core.ssh.integration

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import pe.amantix.tecoterm.core.ssh.connection.ExitStatus
import pe.amantix.tecoterm.core.ssh.crypto.AlgorithmCategory
import pe.amantix.tecoterm.core.ssh.crypto.HostKey
import pe.amantix.tecoterm.core.ssh.crypto.SshAlgorithm
import pe.amantix.tecoterm.core.ssh.transport.HostKeyVerifier
import kotlin.time.Duration.Companion.seconds

internal class AlgorithmMatrixTest {
    @ParameterizedTest
    @MethodSource("intercambios")
    fun `cada intercambio de claves funciona con OpenSSH`(algoritmo: SshAlgorithm) {
        probar(restringir(algoritmo))
    }

    @ParameterizedTest
    @MethodSource("cifradosAead")
    fun `cada cifrado AEAD funciona con OpenSSH`(algoritmo: SshAlgorithm) {
        probar(restringir(algoritmo))
    }

    @ParameterizedTest
    @MethodSource("cifradosConMac")
    fun `cada cifrado CTR con cada MAC funciona con OpenSSH`(combinacion: Pair<SshAlgorithm, SshAlgorithm>) {
        probar(restringir(combinacion.first) intersect restringir(combinacion.second))
    }

    @ParameterizedTest
    @MethodSource("clavesHost")
    fun `cada tipo de clave del servidor se verifica`(algoritmo: SshAlgorithm) {
        var recibida: HostKey? = null
        probar(restringir(algoritmo)) { _, _, clave ->
            recibida = clave
            TestServer.verificadorConfiable.verificar(TestServer.host, TestServer.puerto, clave)
        }
        val tipoEsperado = if (algoritmo.nombre.startsWith("rsa-")) "ssh-rsa" else algoritmo.nombre
        assertEquals(TestServer.clavesHost[tipoEsperado], recibida)
    }

    private fun probar(
        permitidos: Set<SshAlgorithm>,
        verificador: HostKeyVerifier = TestServer.verificadorConfiable,
    ) = runBlocking {
        withTimeout(30.seconds) {
            val conexion =
                TestServer.conectar(
                    config = TestServer.config().copy(algoritmosPermitidos = permitidos),
                    verificador = verificador,
                )
            val resultado = TestServer.ejecutar(conexion, "echo listo")
            conexion.desconectar()
            assertEquals("listo\n", resultado.salidaTexto)
            assertEquals(ExitStatus.Codigo(0), resultado.estado)
        }
    }

    private fun restringir(algoritmo: SshAlgorithm): Set<SshAlgorithm> =
        SshAlgorithm.entries.filter { it.categoria != algoritmo.categoria || it == algoritmo }.toSet()

    companion object {
        @JvmStatic
        fun intercambios(): List<SshAlgorithm> = SshAlgorithm.entries.filter { it.categoria == AlgorithmCategory.INTERCAMBIO_CLAVES }

        @JvmStatic
        fun cifradosAead(): List<SshAlgorithm> = listOf(SshAlgorithm.AES256_GCM, SshAlgorithm.AES128_GCM)

        @JvmStatic
        fun cifradosConMac(): List<Pair<SshAlgorithm, SshAlgorithm>> =
            listOf(SshAlgorithm.AES256_CTR, SshAlgorithm.AES128_CTR).flatMap { cifrado ->
                SshAlgorithm.entries.filter { it.categoria == AlgorithmCategory.MAC }.map { cifrado to it }
            }

        @JvmStatic
        fun clavesHost(): List<SshAlgorithm> = SshAlgorithm.entries.filter { it.categoria == AlgorithmCategory.CLAVE_HOST }
    }
}
