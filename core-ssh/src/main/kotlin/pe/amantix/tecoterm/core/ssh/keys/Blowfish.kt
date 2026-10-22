package pe.amantix.tecoterm.core.ssh.keys

internal class Blowfish private constructor(
    private val p: IntArray,
    private val s: Array<IntArray>,
) {
    fun expandir(
        datos: ByteArray,
        clave: ByteArray,
    ) {
        mezclarClave(clave)
        val flujo = WordStream(datos)
        var izquierda = 0
        var derecha = 0

        fun rellenar(destino: IntArray) {
            for (indice in destino.indices step 2) {
                izquierda = izquierda xor flujo.siguiente()
                derecha = derecha xor flujo.siguiente()
                val bloque = cifrar(izquierda, derecha)
                izquierda = bloque.first
                derecha = bloque.second
                destino[indice] = izquierda
                destino[indice + 1] = derecha
            }
        }
        rellenar(p)
        s.forEach(::rellenar)
    }

    fun expandirSinDatos(clave: ByteArray) {
        mezclarClave(clave)
        var izquierda = 0
        var derecha = 0

        fun rellenar(destino: IntArray) {
            for (indice in destino.indices step 2) {
                val bloque = cifrar(izquierda, derecha)
                izquierda = bloque.first
                derecha = bloque.second
                destino[indice] = izquierda
                destino[indice + 1] = derecha
            }
        }
        rellenar(p)
        s.forEach(::rellenar)
    }

    fun cifrarBloques(palabras: IntArray) {
        for (indice in palabras.indices step 2) {
            val bloque = cifrar(palabras[indice], palabras[indice + 1])
            palabras[indice] = bloque.first
            palabras[indice + 1] = bloque.second
        }
    }

    fun limpiar() {
        p.fill(0)
        s.forEach { it.fill(0) }
    }

    private fun mezclarClave(clave: ByteArray) {
        val flujo = WordStream(clave)
        for (indice in p.indices) {
            p[indice] = p[indice] xor flujo.siguiente()
        }
    }

    private fun cifrar(
        izquierdaInicial: Int,
        derechaInicial: Int,
    ): Pair<Int, Int> {
        var izquierda = izquierdaInicial
        var derecha = derechaInicial
        for (ronda in 0 until RONDAS) {
            izquierda = izquierda xor p[ronda]
            derecha = derecha xor f(izquierda)
            val temporal = izquierda
            izquierda = derecha
            derecha = temporal
        }
        val temporal = izquierda
        izquierda = derecha
        derecha = temporal
        derecha = derecha xor p[RONDAS]
        izquierda = izquierda xor p[RONDAS + 1]
        return izquierda to derecha
    }

    private fun f(valor: Int): Int {
        val a = (valor ushr DESPLAZAMIENTO_A) and MASCARA_BYTE
        val b = (valor ushr DESPLAZAMIENTO_B) and MASCARA_BYTE
        val c = (valor ushr DESPLAZAMIENTO_C) and MASCARA_BYTE
        val d = valor and MASCARA_BYTE
        return ((s[0][a] + s[1][b]) xor s[2][c]) + s[CAJA_CUATRO][d]
    }

    private class WordStream(
        private val datos: ByteArray,
    ) {
        private var posicion: Int = 0

        fun siguiente(): Int {
            var palabra = 0
            repeat(BYTES_PALABRA) {
                palabra = (palabra shl BITS_BYTE) or (datos[posicion].toInt() and MASCARA_BYTE)
                posicion = (posicion + 1) % datos.size
            }
            return palabra
        }
    }

    companion object {
        private const val RONDAS: Int = 16
        private const val TAMANO_P: Int = RONDAS + 2
        private const val CAJAS: Int = 4
        private const val CAJA_CUATRO: Int = 3
        private const val TAMANO_CAJA: Int = 256
        private const val MASCARA_BYTE: Int = 0xFF
        private const val BITS_BYTE: Int = 8
        private const val BYTES_PALABRA: Int = 4
        private const val DESPLAZAMIENTO_A: Int = 24
        private const val DESPLAZAMIENTO_B: Int = 16
        private const val DESPLAZAMIENTO_C: Int = 8

        private val constantes: IntArray by lazy { PiDigits.palabrasFraccionarias(TAMANO_P + CAJAS * TAMANO_CAJA) }

        fun inicial(): Blowfish {
            val digitos = constantes
            return Blowfish(
                p = digitos.copyOfRange(0, TAMANO_P),
                s =
                    Array(CAJAS) { caja ->
                        digitos.copyOfRange(TAMANO_P + caja * TAMANO_CAJA, TAMANO_P + (caja + 1) * TAMANO_CAJA)
                    },
            )
        }

        fun palabrasIniciales(): IntArray = constantes.copyOf()
    }
}
