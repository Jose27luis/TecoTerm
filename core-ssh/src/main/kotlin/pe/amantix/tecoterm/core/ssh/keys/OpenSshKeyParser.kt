package pe.amantix.tecoterm.core.ssh.keys

import pe.amantix.tecoterm.core.ssh.InvalidKeyFileException
import pe.amantix.tecoterm.core.ssh.WrongPassphraseException
import pe.amantix.tecoterm.core.ssh.crypto.EcP256
import pe.amantix.tecoterm.core.ssh.crypto.SecretText
import pe.amantix.tecoterm.core.ssh.wire.SshReader
import java.security.GeneralSecurityException
import java.security.KeyPair
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

internal object OpenSshKeyParser {
    const val TIPO_PEM: String = "OPENSSH PRIVATE KEY"
    private const val MAGIA: String = "openssh-key-v1\u0000"
    private const val SIN_CIFRADO: String = "none"
    private const val KDF_BCRYPT: String = "bcrypt"
    private const val RONDAS_MAXIMAS: Long = 4096
    private const val BYTES_ETIQUETA_GCM: Int = 16
    private const val BITS_ETIQUETA_GCM: Int = 128
    private const val LONGITUD_PRIVADA_ED25519: Int = 64

    private val cifrados: Map<String, KeyFileCipher> =
        mapOf(
            "aes256-ctr" to KeyFileCipher.flujo("AES/CTR/NoPadding", KeyFileSecrets.LONGITUD_AES_256),
            "aes128-ctr" to KeyFileCipher.flujo("AES/CTR/NoPadding", KeyFileSecrets.LONGITUD_AES_128),
            "aes256-cbc" to KeyFileCipher.flujo("AES/CBC/NoPadding", KeyFileSecrets.LONGITUD_AES_256),
            "aes128-cbc" to KeyFileCipher.flujo("AES/CBC/NoPadding", KeyFileSecrets.LONGITUD_AES_128),
            "aes256-gcm@openssh.com" to KeyFileCipher.aead(KeyFileSecrets.LONGITUD_AES_256),
            "aes128-gcm@openssh.com" to KeyFileCipher.aead(KeyFileSecrets.LONGITUD_AES_128),
        )

    fun leer(
        contenido: ByteArray,
        contrasena: CharArray?,
    ): ImportedKey {
        val magia = MAGIA.toByteArray(Charsets.US_ASCII)
        if (contenido.size < magia.size || !contenido.copyOf(magia.size).contentEquals(magia)) {
            throw InvalidKeyFileException("La clave no tiene el formato openssh-key-v1")
        }
        val lector = SshReader(contenido.copyOfRange(magia.size, contenido.size))
        val nombreCifrado = lector.leerTexto()
        val kdf = lector.leerTexto()
        val opcionesKdf = lector.leerString()
        if (lector.leerUint32() != 1L) {
            throw InvalidKeyFileException("Solo se admite una clave por archivo")
        }
        val publica = lector.leerString()
        val cifrada = lector.leerString()
        val cifrado = if (nombreCifrado == SIN_CIFRADO) null else cifradoSoportado(nombreCifrado, kdf)
        val etiqueta = if (cifrado?.aead == true) lector.leerBytes(BYTES_ETIQUETA_GCM) else ByteArray(0)
        lector.verificarFin()
        val privada = if (cifrado == null) cifrada else descifrar(cifrado, opcionesKdf, cifrada + etiqueta, contrasena)
        try {
            return leerSeccionPrivada(privada, publica, estabaCifrada = cifrado != null)
        } finally {
            privada.fill(0)
        }
    }

    private fun cifradoSoportado(
        nombre: String,
        kdf: String,
    ): KeyFileCipher {
        if (kdf != KDF_BCRYPT) {
            throw InvalidKeyFileException("Derivación de clave no soportada: $kdf")
        }
        return cifrados[nombre] ?: throw InvalidKeyFileException("Cifrado de clave no soportado: $nombre")
    }

    private fun descifrar(
        cifrado: KeyFileCipher,
        opcionesKdf: ByteArray,
        datos: ByteArray,
        contrasena: CharArray?,
    ): ByteArray {
        val passphrase = KeyFileSecrets.exigirPassphrase(contrasena)
        val opciones = SshReader(opcionesKdf)
        val sal = opciones.leerString()
        val rondas = opciones.leerUint32()
        opciones.verificarFin()
        if (rondas < 1 || rondas > RONDAS_MAXIMAS) {
            throw InvalidKeyFileException("Cantidad de rondas de bcrypt fuera de rango: $rondas")
        }
        val bytesContrasena = SecretText.utf8(passphrase)
        val material = BcryptPbkdf.derivar(bytesContrasena, sal, rondas.toInt(), cifrado.longitudClave + cifrado.longitudIv)
        bytesContrasena.fill(0)
        val clave = SecretKeySpec(material, 0, cifrado.longitudClave, "AES")
        val iv = material.copyOfRange(cifrado.longitudClave, material.size)
        material.fill(0)
        return try {
            Cipher.getInstance(cifrado.transformacion).run {
                init(Cipher.DECRYPT_MODE, clave, if (cifrado.aead) GCMParameterSpec(BITS_ETIQUETA_GCM, iv) else IvParameterSpec(iv))
                doFinal(datos)
            }
        } catch (_: GeneralSecurityException) {
            throw WrongPassphraseException()
        }
    }

    private fun leerSeccionPrivada(
        privada: ByteArray,
        publicaExterna: ByteArray,
        estabaCifrada: Boolean,
    ): ImportedKey {
        val lector = SshReader(privada)
        if (lector.leerUint32() != lector.leerUint32()) {
            throw if (estabaCifrada) WrongPassphraseException() else InvalidKeyFileException("La sección privada está dañada")
        }
        val par = leerPar(lector.leerTexto(), lector)
        val comentario = lector.leerTexto()
        exigirRelleno(lector)
        val clave = KeyPairFactory.validar(par)
        if (!clave.blobPublico.contentEquals(publicaExterna)) {
            throw InvalidKeyFileException("La clave pública del archivo no coincide con la privada")
        }
        return ImportedKey(clave, comentario)
    }

    private fun leerPar(
        tipo: String,
        lector: SshReader,
    ): KeyPair =
        when (tipo) {
            "ssh-ed25519" -> {
                val publica = lector.leerString()
                val privada = lector.leerString()
                if (privada.size != LONGITUD_PRIVADA_ED25519) {
                    throw InvalidKeyFileException("Clave privada Ed25519 de ${privada.size} bytes")
                }
                val semilla = privada.copyOf(privada.size / 2)
                privada.fill(0)
                try {
                    KeyPairFactory.ed25519(semilla, publica)
                } finally {
                    semilla.fill(0)
                }
            }

            "ssh-rsa" -> {
                val modulo = lector.leerMpint()
                val exponentePublico = lector.leerMpint()
                val exponentePrivado = lector.leerMpint()
                lector.leerMpint()
                val primoP = lector.leerMpint()
                val primoQ = lector.leerMpint()
                KeyPairFactory.rsa(RsaComponents(modulo, exponentePublico, exponentePrivado, primoP, primoQ))
            }

            "ecdsa-sha2-nistp256" -> {
                val curva = lector.leerTexto()
                val punto = lector.leerString()
                val privado = lector.leerMpint()
                if (curva != EcP256.NOMBRE_CURVA) {
                    throw InvalidKeyFileException("Curva no soportada: $curva")
                }
                KeyPairFactory.ecdsaP256(privado, punto)
            }

            else -> {
                throw InvalidKeyFileException("Tipo de clave no soportado: $tipo")
            }
        }

    private fun exigirRelleno(lector: SshReader) {
        var esperado = 1
        while (lector.restantes > 0) {
            if (lector.leerByte() != esperado) {
                throw InvalidKeyFileException("Relleno de la sección privada inválido")
            }
            esperado++
        }
    }

    private class KeyFileCipher(
        val transformacion: String,
        val longitudClave: Int,
        val longitudIv: Int,
        val aead: Boolean,
    ) {
        companion object {
            fun flujo(
                transformacion: String,
                longitudClave: Int,
            ): KeyFileCipher = KeyFileCipher(transformacion, longitudClave, KeyFileSecrets.LONGITUD_IV_AES, aead = false)

            fun aead(longitudClave: Int): KeyFileCipher =
                KeyFileCipher("AES/GCM/NoPadding", longitudClave, KeyFileSecrets.LONGITUD_NONCE_GCM, aead = true)
        }
    }
}
