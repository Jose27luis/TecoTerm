package pe.amantix.tecoterm.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NoGlobalScopeRuleTest {
    private val regla = NoGlobalScopeRule(Config.empty)

    @Test
    fun `reporta GlobalScope`() {
        val codigo = "fun iniciar() { GlobalScope.launch { } }"

        assertEquals(1, regla.lint(codigo).size)
    }

    @Test
    fun `acepta un alcance propio`() {
        val codigo = "fun iniciar(alcance: CoroutineScope) { alcance.launch { } }"

        assertEquals(0, regla.lint(codigo).size)
    }
}
