package pe.amantix.tecoterm.core.ssh.crypto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pe.amantix.tecoterm.core.ssh.SshProtocolException
import java.math.BigInteger

class EcdsaSignatureCodecTest {
    @Test
    fun `r y s van y vuelven por DER`() {
        val casos =
            listOf(
                BigInteger.ONE to BigInteger.valueOf(255),
                BigInteger("ff".repeat(32), 16) to BigInteger("7f".repeat(32), 16),
            )

        casos.forEach { (r, s) ->
            assertEquals(r to s, EcdsaSignatureCodec.rsDesdeDer(EcdsaSignatureCodec.derDesdeRs(r, s)))
        }
    }

    @Test
    fun `un entero con el bit alto lleva un cero delante`() {
        val der = EcdsaSignatureCodec.derDesdeRs(BigInteger.valueOf(0x80), BigInteger.ONE)

        assertEquals("3007020200800201" + "01", der.joinToString("") { "%02x".format(it) })
    }

    @Test
    fun `un DER truncado se rechaza`() {
        val der = EcdsaSignatureCodec.derDesdeRs(BigInteger.TEN, BigInteger.TEN)

        assertThrows<SshProtocolException> { EcdsaSignatureCodec.rsDesdeDer(der.copyOf(der.size - 1)) }
    }
}
