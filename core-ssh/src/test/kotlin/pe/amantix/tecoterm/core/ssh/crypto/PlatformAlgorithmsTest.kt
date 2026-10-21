package pe.amantix.tecoterm.core.ssh.crypto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pe.amantix.tecoterm.core.ssh.crypto.PlatformPrimitive.CipherPrimitive
import pe.amantix.tecoterm.core.ssh.crypto.PlatformPrimitive.KeyAgreementPrimitive

class PlatformAlgorithmsTest {
    @Test
    fun `la JVM de referencia soporta todos los algoritmos`() {
        val faltantes = PlatformAlgorithms.detectar().filterNot(AlgorithmAvailability::disponible)

        assertEquals(emptyList<AlgorithmAvailability>(), faltantes)
    }

    @Test
    fun `disponibles respeta la categoria y el orden de preferencia`() {
        val intercambio = PlatformAlgorithms.disponibles(AlgorithmCategory.INTERCAMBIO_CLAVES)

        assertEquals(
            listOf(
                SshAlgorithm.CURVE25519_SHA256,
                SshAlgorithm.CURVE25519_SHA256_LIBSSH,
                SshAlgorithm.ECDH_SHA2_NISTP256,
            ),
            intercambio,
        )
    }

    @Test
    fun `cada categoria tiene al menos un algoritmo`() {
        AlgorithmCategory.entries.forEach { categoria ->
            assertTrue(PlatformAlgorithms.disponibles(categoria).isNotEmpty(), categoria.name)
        }
    }

    @Test
    fun `una primitiva inexistente no esta disponible`() {
        assertFalse(CipherPrimitive("Inexistente/NoPadding").estaDisponible())
        assertFalse(KeyAgreementPrimitive("Inexistente").estaDisponible())
    }

    @Test
    fun `los nombres de algoritmo son unicos`() {
        val nombres = SshAlgorithm.entries.map(SshAlgorithm::nombre)

        assertEquals(nombres.size, nombres.toSet().size)
    }
}
