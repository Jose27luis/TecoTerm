package pe.amantix.tecoterm.core.ssh.keys

import pe.amantix.tecoterm.core.ssh.InvalidKeyFileException
import pe.amantix.tecoterm.core.ssh.WrongPassphraseException
import java.math.BigInteger
import java.security.KeyPair

internal object PemKeyParser {
    private const val TIPO_PKCS8: String = "PRIVATE KEY"
    private const val TIPO_PKCS8_CIFRADO: String = "ENCRYPTED PRIVATE KEY"
    private const val TIPO_RSA: String = "RSA PRIVATE KEY"
    private const val TIPO_EC: String = "EC PRIVATE KEY"
    private const val OID_RSA: String = "1.2.840.113549.1.1.1"
    private const val OID_EC: String = "1.2.840.10045.2.1"
    private const val OID_P256: String = "1.2.840.10045.3.1.7"
    private const val OID_ED25519: String = "1.3.101.112"
    private const val CONTEXTO_PARAMETROS: Int = 0
    private const val CONTEXTO_PUBLICA: Int = 1

    fun leer(
        bloque: PemBlock,
        contrasena: CharArray?,
    ): ImportedKey {
        val cifrado = bloque.tipo == TIPO_PKCS8_CIFRADO || LegacyPemDecryptor.estaCifrado(bloque)
        val der =
            when {
                bloque.tipo == TIPO_PKCS8_CIFRADO -> Pbes2Decryptor.descifrar(bloque.contenido, contrasena)
                LegacyPemDecryptor.estaCifrado(bloque) -> LegacyPemDecryptor.descifrar(bloque, contrasena)
                else -> bloque.contenido
            }
        try {
            val par =
                try {
                    leerPar(bloque.tipo, der)
                } catch (error: InvalidKeyFileException) {
                    throw if (cifrado) WrongPassphraseException() else error
                }
            return ImportedKey(KeyPairFactory.validar(par), "")
        } finally {
            der.fill(0)
        }
    }

    private fun leerPar(
        tipo: String,
        der: ByteArray,
    ): KeyPair =
        when (tipo) {
            TIPO_PKCS8, TIPO_PKCS8_CIFRADO -> pkcs8(der)
            TIPO_RSA -> pkcs1(der)
            TIPO_EC -> sec1(der)
            else -> throw InvalidKeyFileException("Formato de clave no soportado: $tipo")
        }

    private fun pkcs8(der: ByteArray): KeyPair {
        val info = DerReader(der).leerSecuencia()
        info.leerEntero()
        val algoritmo = info.leerSecuencia()
        val oid = algoritmo.leerOid()
        val privada = info.leerOctetos()
        return when (oid) {
            OID_RSA -> {
                pkcs1(privada)
            }

            OID_EC -> {
                if (algoritmo.leerOid() != OID_P256) {
                    throw InvalidKeyFileException("Solo se admiten claves EC en la curva P-256")
                }
                sec1(privada)
            }

            OID_ED25519 -> {
                ed25519(privada, info)
            }

            else -> {
                throw InvalidKeyFileException("Algoritmo de clave no soportado: $oid")
            }
        }
    }

    private fun pkcs1(der: ByteArray): KeyPair {
        val clave = DerReader(der).leerSecuencia()
        clave.leerEntero()
        return KeyPairFactory.rsa(
            RsaComponents(
                modulo = clave.leerEntero(),
                exponentePublico = clave.leerEntero(),
                exponentePrivado = clave.leerEntero(),
                primoP = clave.leerEntero(),
                primoQ = clave.leerEntero(),
            ),
        )
    }

    private fun sec1(der: ByteArray): KeyPair {
        val clave = DerReader(der).leerSecuencia()
        clave.leerEntero()
        val privado = BigInteger(1, clave.leerOctetos())
        var publica: ByteArray? = null
        while (clave.tieneMas) {
            when (clave.siguienteEtiqueta) {
                DerReader.ETIQUETA_CONTEXTO or CONTEXTO_PARAMETROS -> {
                    if (clave.leerContexto(CONTEXTO_PARAMETROS).leerOid() != OID_P256) {
                        throw InvalidKeyFileException("Solo se admiten claves EC en la curva P-256")
                    }
                }

                DerReader.ETIQUETA_CONTEXTO or CONTEXTO_PUBLICA -> {
                    publica = clave.leerContexto(CONTEXTO_PUBLICA).leerBits()
                }

                else -> {
                    clave.saltar()
                }
            }
        }
        return KeyPairFactory.ecdsaP256(
            privado,
            publica ?: throw InvalidKeyFileException("La clave EC no incluye su clave pública"),
        )
    }

    private fun ed25519(
        privada: ByteArray,
        info: DerReader,
    ): KeyPair {
        val semilla = DerReader(privada).leerOctetos()
        var publica: ByteArray? = null
        while (info.tieneMas) {
            if (info.siguienteEtiqueta == (DerReader.ETIQUETA_CONTEXTO or CONTEXTO_PUBLICA)) {
                publica = info.leerContexto(CONTEXTO_PUBLICA).leerBits()
            } else {
                info.saltar()
            }
        }
        val crudo =
            publica
                ?: throw InvalidKeyFileException("La clave Ed25519 en PKCS#8 no incluye su clave pública: expórtela en formato OpenSSH")
        try {
            return KeyPairFactory.ed25519(semilla, crudo)
        } finally {
            semilla.fill(0)
        }
    }
}
