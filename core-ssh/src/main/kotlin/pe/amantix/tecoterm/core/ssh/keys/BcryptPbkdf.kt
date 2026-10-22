package pe.amantix.tecoterm.core.ssh.keys

import java.security.MessageDigest

internal object BcryptPbkdf {
    private const val LONGITUD_HASH: Int = 32
    private const val PALABRAS_HASH: Int = LONGITUD_HASH / 4
    private const val RONDAS_EXPANSION: Int = 64
    private const val RONDAS_CIFRADO: Int = 64
    private const val BYTES_CONTADOR: Int = 4
    private const val BITS_BYTE: Int = 8
    private const val MASCARA_BYTE: Int = 0xFF
    private val textoMagico: ByteArray = "OxychromaticBlowfishSwatDynamite".toByteArray(Charsets.US_ASCII)

    fun derivar(
        contrasena: ByteArray,
        sal: ByteArray,
        rondas: Int,
        longitud: Int,
    ): ByteArray {
        require(rondas > 0 && longitud > 0 && longitud <= LONGITUD_HASH * LONGITUD_HASH) { "Parámetros de bcrypt-pbkdf inválidos" }
        val salto = (longitud + LONGITUD_HASH - 1) / LONGITUD_HASH
        val porBloque = (longitud + salto - 1) / salto
        val sha512 = MessageDigest.getInstance("SHA-512")
        val resumenContrasena = sha512.digest(contrasena)
        val clave = ByteArray(longitud)
        var pendientes = longitud
        var contador = 1
        while (pendientes > 0) {
            val salConContador = sal + bytesContador(contador)
            var resumenSal = sha512.digest(salConContador)
            var intermedio = hash(resumenContrasena, resumenSal)
            val acumulado = intermedio.copyOf()
            repeat(rondas - 1) {
                resumenSal = sha512.digest(intermedio)
                intermedio = hash(resumenContrasena, resumenSal)
                for (indice in acumulado.indices) {
                    acumulado[indice] = (acumulado[indice].toInt() xor intermedio[indice].toInt()).toByte()
                }
            }
            var copiados = 0
            while (copiados < minOf(porBloque, pendientes)) {
                val destino = copiados * salto + (contador - 1)
                if (destino >= longitud) {
                    break
                }
                clave[destino] = acumulado[copiados]
                copiados++
            }
            pendientes -= copiados
            contador++
            acumulado.fill(0)
            intermedio.fill(0)
        }
        resumenContrasena.fill(0)
        return clave
    }

    private fun hash(
        resumenContrasena: ByteArray,
        resumenSal: ByteArray,
    ): ByteArray {
        val estado = Blowfish.inicial()
        estado.expandir(resumenSal, resumenContrasena)
        repeat(RONDAS_EXPANSION) {
            estado.expandirSinDatos(resumenSal)
            estado.expandirSinDatos(resumenContrasena)
        }
        val palabras =
            IntArray(PALABRAS_HASH) { indice ->
                var palabra = 0
                for (desplazamiento in 0 until BYTES_CONTADOR) {
                    palabra = (palabra shl BITS_BYTE) or (textoMagico[indice * BYTES_CONTADOR + desplazamiento].toInt() and MASCARA_BYTE)
                }
                palabra
            }
        repeat(RONDAS_CIFRADO) { estado.cifrarBloques(palabras) }
        estado.limpiar()
        val salida = ByteArray(LONGITUD_HASH)
        palabras.forEachIndexed { indice, palabra ->
            for (desplazamiento in 0 until BYTES_CONTADOR) {
                salida[indice * BYTES_CONTADOR + desplazamiento] = (palabra ushr (BITS_BYTE * desplazamiento)).toByte()
            }
        }
        palabras.fill(0)
        return salida
    }

    private fun bytesContador(contador: Int): ByteArray =
        ByteArray(BYTES_CONTADOR) { indice -> (contador ushr (BITS_BYTE * (BYTES_CONTADOR - 1 - indice))).toByte() }
}
