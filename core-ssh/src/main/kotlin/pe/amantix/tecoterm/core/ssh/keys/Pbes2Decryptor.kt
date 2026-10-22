package pe.amantix.tecoterm.core.ssh.keys

import pe.amantix.tecoterm.core.ssh.InvalidKeyFileException
import pe.amantix.tecoterm.core.ssh.WrongPassphraseException
import java.math.BigInteger
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

internal object Pbes2Decryptor {
    private const val OID_PBES2: String = "1.2.840.113549.1.5.13"
    private const val OID_PBKDF2: String = "1.2.840.113549.1.5.12"
    private const val OID_HMAC_SHA1: String = "1.2.840.113549.2.7"
    private const val ITERACIONES_MAXIMAS: Long = 10_000_000
    private const val BITS_BYTE: Int = 8
    private val funcionesPrf: Map<String, String> =
        mapOf(
            OID_HMAC_SHA1 to "PBKDF2WithHmacSHA1",
            "1.2.840.113549.2.9" to "PBKDF2WithHmacSHA256",
            "1.2.840.113549.2.11" to "PBKDF2WithHmacSHA512",
        )
    private val longitudesAes: Map<String, Int> =
        mapOf(
            "2.16.840.1.101.3.4.1.2" to KeyFileSecrets.LONGITUD_AES_128,
            "2.16.840.1.101.3.4.1.22" to KeyFileSecrets.LONGITUD_AES_192,
            "2.16.840.1.101.3.4.1.42" to KeyFileSecrets.LONGITUD_AES_256,
        )

    fun descifrar(
        contenido: ByteArray,
        contrasena: CharArray?,
    ): ByteArray {
        val info = DerReader(contenido).leerSecuencia()
        val parametros = leerParametros(info.leerSecuencia())
        val cifrado = info.leerOctetos()
        val especificacion =
            PBEKeySpec(
                KeyFileSecrets.exigirPassphrase(contrasena),
                parametros.sal,
                parametros.iteraciones,
                parametros.longitudClave * BITS_BYTE,
            )
        return try {
            val clave = SecretKeyFactory.getInstance(parametros.funcion).generateSecret(especificacion).encoded
            try {
                Cipher.getInstance("AES/CBC/PKCS5Padding").run {
                    init(Cipher.DECRYPT_MODE, SecretKeySpec(clave, "AES"), IvParameterSpec(parametros.iv))
                    doFinal(cifrado)
                }
            } finally {
                clave.fill(0)
            }
        } catch (_: GeneralSecurityException) {
            throw WrongPassphraseException()
        } finally {
            especificacion.clearPassword()
        }
    }

    private fun leerParametros(algoritmo: DerReader): Pbes2Parameters {
        if (algoritmo.leerOid() != OID_PBES2) {
            throw InvalidKeyFileException("Solo se admiten claves PKCS#8 cifradas con PBES2")
        }
        val parametros = algoritmo.leerSecuencia()
        val derivacion = parametros.leerSecuencia()
        if (derivacion.leerOid() != OID_PBKDF2) {
            throw InvalidKeyFileException("Solo se admite PBKDF2 en claves PKCS#8 cifradas")
        }
        val pbkdf2 = derivacion.leerSecuencia()
        val sal = pbkdf2.leerOctetos()
        val iteraciones = iteracionesValidas(pbkdf2.leerEntero())
        if (pbkdf2.siguienteEtiqueta == DerReader.ETIQUETA_ENTERO) {
            pbkdf2.leerEntero()
        }
        val prf = if (pbkdf2.tieneMas) pbkdf2.leerSecuencia().leerOid() else OID_HMAC_SHA1
        val esquema = parametros.leerSecuencia()
        val oidCifrado = esquema.leerOid()
        return Pbes2Parameters(
            sal = sal,
            iteraciones = iteraciones,
            funcion = soportado(funcionesPrf, prf, "Función PRF de PBKDF2"),
            longitudClave = soportado(longitudesAes, oidCifrado, "Cifrado PKCS#8"),
            iv = esquema.leerOctetos(),
        )
    }

    private fun iteracionesValidas(iteraciones: BigInteger): Int {
        if (iteraciones.signum() <= 0 || iteraciones > BigInteger.valueOf(ITERACIONES_MAXIMAS)) {
            throw InvalidKeyFileException("Iteraciones de PBKDF2 fuera de rango")
        }
        return iteraciones.toInt()
    }

    private fun <T> soportado(
        tabla: Map<String, T>,
        oid: String,
        descripcion: String,
    ): T = tabla[oid] ?: throw InvalidKeyFileException("$descripcion no soportado: $oid")

    private class Pbes2Parameters(
        val sal: ByteArray,
        val iteraciones: Int,
        val funcion: String,
        val longitudClave: Int,
        val iv: ByteArray,
    )
}
