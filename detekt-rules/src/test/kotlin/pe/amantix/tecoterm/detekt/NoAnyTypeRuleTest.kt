package pe.amantix.tecoterm.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NoAnyTypeRuleTest {
    private val regla = NoAnyTypeRule(Config.empty)

    @Test
    fun `reporta Any en parametros, retornos y genericos`() {
        val codigo =
            """
            fun registrar(valor: Any): Any? = valor
            val datos: Map<String, Any> = emptyMap()
            """.trimIndent()

        assertEquals(3, regla.lint(codigo).size)
    }

    @Test
    fun `reporta Any calificado con kotlin`() {
        val codigo = "val valor: kotlin.Any = 1"

        assertEquals(1, regla.lint(codigo).size)
    }

    @Test
    fun `acepta Any como cota de un generico`() {
        val codigo =
            """
            fun <T : Any> primero(lista: List<T>): T = lista.first()
            fun <T> ultimo(lista: List<T>): T where T : Any = lista.last()
            """.trimIndent()

        assertEquals(0, regla.lint(codigo).size)
    }

    @Test
    fun `acepta el parametro de equals sobrescrito`() {
        val codigo =
            """
            class Huella(val bytes: ByteArray) {
                override fun equals(other: Any?): Boolean = other is Huella && bytes.contentEquals(other.bytes)
                override fun hashCode(): Int = bytes.contentHashCode()
            }
            """.trimIndent()

        assertEquals(0, regla.lint(codigo).size)
    }
}
