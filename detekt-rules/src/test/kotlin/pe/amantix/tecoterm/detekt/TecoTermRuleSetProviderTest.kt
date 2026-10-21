package pe.amantix.tecoterm.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSetProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.ServiceLoader

class TecoTermRuleSetProviderTest {
    @Test
    fun `el proveedor se registra como servicio de detekt`() {
        val proveedores = ServiceLoader.load(RuleSetProvider::class.java).map { it.ruleSetId }

        assertEquals(listOf("tecoterm"), proveedores.filter { it == "tecoterm" })
    }

    @Test
    fun `el conjunto incluye las siete reglas`() {
        val reglas = TecoTermRuleSetProvider().instance(Config.empty).rules

        assertEquals(7, reglas.size)
    }
}
