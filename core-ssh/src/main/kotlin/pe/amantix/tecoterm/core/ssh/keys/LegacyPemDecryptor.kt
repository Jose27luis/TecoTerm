package pe.amantix.tecoterm.core.ssh.keys

import pe.amantix.tecoterm.core.ssh.InvalidKeyFileException
import pe.amantix.tecoterm.core.ssh.WrongPassphraseException
import pe.amantix.tecoterm.core.ssh.crypto.SecretText
import java.security.GeneralSecurityException
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

internal object LegacyPemDecryptor {
    private const val CABECERA_TIPO: String = "Proc-Type"
    private const val CABECERA_DEK: String = "DEK-Info"
    private const val MARCA_CIFRADA: String = "ENCRYPTED"
    private const val LONGITUD_SAL: Int = 8
    private const val BASE_HEXADECIMAL: Int = 16
    private val longitudesClave: Map<String, Int> =
        mapOf(
            "AES-128-CBC" to KeyFileSecrets.LONGITUD_AES_128,
            "AES-192-CBC" to KeyFileSecrets.LONGITUD_AES_192,
            "AES-256-CBC" to KeyFileSecrets.LONGITUD_AES_256,
        )

    fun estaCifrado(bloque: PemBlock): Boolean = bloque.cabeceras[CABECERA_TIPO]?.contains(MARCA_CIFRADA) == true

    fun descifrar(
        bloque: PemBlock,
        contrasena: CharArray?,
    ): ByteArray {
        val (longitudClave, iv) = leerDek(bloque)
        val bytesContrasena = SecretText.utf8(KeyFileSecrets.exigirPassphrase(contrasena))
        val clave = derivarClave(bytesContrasena, iv.copyOf(LONGITUD_SAL), longitudClave)
        bytesContrasena.fill(0)
        return try {
            Cipher.getInstance("AES/CBC/PKCS5Padding").run {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(clave, "AES"), IvParameterSpec(iv))
                doFinal(bloque.contenido)
            }
        } catch (_: GeneralSecurityException) {
            throw WrongPassphraseException()
        } finally {
            clave.fill(0)
        }
    }

    private fun leerDek(bloque: PemBlock): Pair<Int, ByteArray> {
        val dek = bloque.cabeceras[CABECERA_DEK] ?: throw InvalidKeyFileException("Falta la cabecera DEK-Info")
        val algoritmo = dek.substringBefore(',').trim()
        val longitudClave = longitudesClave[algoritmo] ?: throw InvalidKeyFileException("Cifrado PEM no soportado: $algoritmo")
        val iv =
            dek
                .substringAfter(',')
                .trim()
                .chunked(2)
                .map { it.toInt(BASE_HEXADECIMAL).toByte() }
                .toByteArray()
        return longitudClave to iv
    }

    private fun derivarClave(
        contrasena: ByteArray,
        sal: ByteArray,
        longitud: Int,
    ): ByteArray {
        val md5 = MessageDigest.getInstance("MD5")
        var resultado = ByteArray(0)
        var anterior = ByteArray(0)
        while (resultado.size < longitud) {
            md5.update(anterior)
            md5.update(contrasena)
            md5.update(sal)
            anterior = md5.digest()
            resultado += anterior
        }
        val clave = resultado.copyOf(longitud)
        resultado.fill(0)
        anterior.fill(0)
        return clave
    }
}
