package pe.amantix.tecoterm.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NoLateinitRuleTest {
    private val regla = NoLateinitRule(Config.empty)

    @Test
    fun `reporta lateinit`() {
        val codigo =
            """
            class Sesion {
                lateinit var usuario: String
            }
            """.trimIndent()

        assertEquals(1, regla.lint(codigo).size)
    }

    @Test
    fun `acepta propiedades inicializadas`() {
        val codigo =
            """
            class Sesion(val usuario: String) {
                var intentos: Int = 0
            }
            """.trimIndent()

        assertEquals(0, regla.lint(codigo).size)
    }
}
