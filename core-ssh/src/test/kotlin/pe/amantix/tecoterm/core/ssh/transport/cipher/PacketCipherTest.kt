package pe.amantix.tecoterm.core.ssh.transport.cipher

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import pe.amantix.tecoterm.core.ssh.SshProtocolException
import pe.amantix.tecoterm.core.ssh.crypto.SshAlgorithm
import pe.amantix.tecoterm.core.ssh.transport.DirectionAlgorithms
import pe.amantix.tecoterm.core.ssh.transport.KeyDerivation
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.security.SecureRandom

internal class PacketCipherTest {
    private val aleatorio = SecureRandom()

    @ParameterizedTest
    @MethodSource("combinaciones")
    fun `los paquetes cifrados van y vuelven`(algoritmos: DirectionAlgorithms) {
        val (emisor, receptor) = par(algoritmos)
        val payloads = listOf(ByteArray(1) { 5 }, ByteArray(15) { 1 }, ByteArray(16) { 2 }, ByteArray(1000) { it.toByte() })
        val flujo = ByteArrayOutputStream()
        payloads.forEachIndexed { secuencia, payload -> flujo.write(emisor.empaquetar(payload, secuencia)) }

        val entrada = DataInputStream(ByteArrayInputStream(flujo.toByteArray()))
        payloads.forEachIndexed { secuencia, payload ->
            assertArrayEquals(payload, receptor.leer(entrada, secuencia))
        }
        assertEquals(0, entrada.available())
    }

    @ParameterizedTest
    @MethodSource("combinaciones")
    fun `un paquete alterado se rechaza`(algoritmos: DirectionAlgorithms) {
        val (emisor, receptor) = par(algoritmos)
        val paquete = emisor.empaquetar(ByteArray(64) { 9 }, 0)
        paquete[paquete.size - 1] = (paquete[paquete.size - 1].toInt() xor 1).toByte()

        assertThrows<SshProtocolException> { receptor.leer(DataInputStream(ByteArrayInputStream(paquete)), 0) }
    }

    @ParameterizedTest
    @MethodSource("combinaciones")
    fun `un numero de secuencia distinto se rechaza`(algoritmos: DirectionAlgorithms) {
        if (algoritmos.mac == null) {
            return
        }
        val (emisor, receptor) = par(algoritmos)
        val paquete = emisor.empaquetar(ByteArray(32) { 3 }, 7)

        assertThrows<SshProtocolException> { receptor.leer(DataInputStream(ByteArrayInputStream(paquete)), 8) }
    }

    private fun par(algoritmos: DirectionAlgorithms): Pair<PacketCipher, PacketCipher> {
        val secreto = ByteArray(32).also(aleatorio::nextBytes)
        val hash = ByteArray(32).also(aleatorio::nextBytes)
        val fabrica = PacketCipherFactory(aleatorio)
        val emisor = fabrica.crear(algoritmos, KeyDerivation(secreto, hash, hash), DerivationLetters.clienteServidor)
        val receptor = fabrica.crear(algoritmos, KeyDerivation(secreto, hash, hash), DerivationLetters.clienteServidor)
        return emisor to receptor
    }

    companion object {
        @JvmStatic
        fun combinaciones(): List<DirectionAlgorithms> {
            val macs =
                listOf(
                    SshAlgorithm.HMAC_SHA2_256_ETM,
                    SshAlgorithm.HMAC_SHA2_512_ETM,
                    SshAlgorithm.HMAC_SHA2_256,
                    SshAlgorithm.HMAC_SHA2_512,
                )
            return listOf(
                DirectionAlgorithms(SshAlgorithm.AES256_GCM, null),
                DirectionAlgorithms(SshAlgorithm.AES128_GCM, null),
            ) +
                listOf(SshAlgorithm.AES256_CTR, SshAlgorithm.AES128_CTR).flatMap { cifrado ->
                    macs.map { DirectionAlgorithms(cifrado, it) }
                }
        }
    }
}
