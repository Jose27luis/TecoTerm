package pe.amantix.tecoterm.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NoSuppressRuleTest {
    private val regla = NoSuppressRule(Config.empty)

    @Test
    fun `reporta Suppress y SuppressWarnings`() {
        val codigo =
            """
            @Suppress("UNUSED")
            fun primero(): Int = 1

            @SuppressWarnings("unchecked")
            fun segundo(): Int = 2
            """.trimIndent()

        assertEquals(2, regla.lint(codigo).size)
    }

    @Test
    fun `acepta otras anotaciones`() {
        val codigo =
            """
            @JvmStatic
            fun primero(): Int = 1
            """.trimIndent()

        assertEquals(0, regla.lint(codigo).size)
    }
}
