package pe.amantix.tecoterm.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NoUnsafeCastRuleTest {
    private val regla = NoUnsafeCastRule(Config.empty)

    @Test
    fun `reporta el cast con as`() {
        val codigo = "fun convertir(valor: Number): Int = valor as Int"

        assertEquals(1, regla.lint(codigo).size)
    }

    @Test
    fun `acepta el cast seguro con as interrogacion`() {
        val codigo = "fun convertir(valor: Number): Int? = valor as? Int"

        assertEquals(0, regla.lint(codigo).size)
    }

    @Test
    fun `acepta el alias de import`() {
        val codigo = "import kotlin.collections.List as Lista"

        assertEquals(0, regla.lint(codigo).size)
    }
}
