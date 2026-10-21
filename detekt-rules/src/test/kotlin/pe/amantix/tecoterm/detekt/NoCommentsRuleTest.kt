package pe.amantix.tecoterm.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NoCommentsRuleTest {
    private val regla = NoCommentsRule(Config.empty)

    @Test
    fun `reporta comentarios de linea, de bloque y KDoc`() {
        val codigo =
            """
            /** Documentación */
            class Ejemplo {
                // de línea
                /* de bloque */
                fun calcular(): Int = 1
            }
            """.trimIndent()

        assertEquals(3, regla.lint(codigo).size)
    }

    @Test
    fun `acepta codigo sin comentarios`() {
        val codigo =
            """
            class Ejemplo {
                fun calcular(): Int = 1
            }
            """.trimIndent()

        assertEquals(0, regla.lint(codigo).size)
    }

    @Test
    fun `no confunde barras dentro de un texto con un comentario`() {
        val codigo = "val direccion: String = \"https://ejemplo.pe/ruta\""

        assertEquals(0, regla.lint(codigo).size)
    }
}
