package pe.amantix.tecoterm.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NoNotNullAssertionRuleTest {
    private val regla = NoNotNullAssertionRule(Config.empty)

    @Test
    fun `reporta el operador doble exclamacion`() {
        val codigo = "fun largo(texto: String?): Int = texto!!.length"

        assertEquals(1, regla.lint(codigo).size)
    }

    @Test
    fun `acepta llamada segura y elvis`() {
        val codigo = "fun largo(texto: String?): Int = texto?.length ?: 0"

        assertEquals(0, regla.lint(codigo).size)
    }
}
