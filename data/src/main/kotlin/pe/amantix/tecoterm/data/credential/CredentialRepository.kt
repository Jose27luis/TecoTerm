package pe.amantix.tecoterm.data.credential

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import pe.amantix.tecoterm.core.security.CredentialCipher
import pe.amantix.tecoterm.core.security.CredentialUnavailableException
import pe.amantix.tecoterm.core.ssh.auth.Credential
import pe.amantix.tecoterm.core.ssh.crypto.ClientKey
import pe.amantix.tecoterm.core.ssh.crypto.SecretText
import pe.amantix.tecoterm.core.ssh.keys.ClientKeyGenerator
import pe.amantix.tecoterm.core.ssh.keys.PrivateKeyImporter
import java.util.Base64
import java.util.UUID

public class CredentialRepository internal constructor(
    private val dao: CredentialDao,
    private val cifrador: CredentialCipher,
    private val reloj: () -> Long,
) {
    public val credenciales: Flow<List<CredentialSummary>> = dao.observar().map { lista -> lista.map(::resumen) }

    public suspend fun importarClave(
        nombre: String,
        texto: String,
        passphrase: CharArray?,
    ): CredentialSummary {
        val importada = withContext(Dispatchers.Default) { PrivateKeyImporter.importar(texto, passphrase) }
        return guardarClave(nombre, importada.clave)
    }

    public suspend fun generarClave(nombre: String): CredentialSummary =
        guardarClave(nombre, withContext(Dispatchers.Default) { ClientKeyGenerator.generar() })

    public suspend fun guardarContrasena(
        nombre: String,
        contrasena: CharArray,
    ): CredentialSummary {
        val id = UUID.randomUUID().toString()
        val bytes = SecretText.utf8(contrasena)
        contrasena.fill('\u0000')
        val contenido = withContext(Dispatchers.IO) { cifrador.cifrarYBorrar(bytes, id) }
        val entidad =
            CredentialEntity(
                id = id,
                tipo = CredentialKind.CONTRASENA,
                nombre = nombre,
                tipoClave = null,
                clavePublica = null,
                huella = null,
                contenidoCifrado = contenido,
                aliasKeystore = cifrador.aliasKeystore,
                creadaEn = reloj(),
            )
        dao.insertar(entidad)
        return resumen(entidad)
    }

    public suspend fun credencialParaConexion(id: String): Credential {
        val entidad = dao.obtener(id) ?: throw CredentialUnavailableException("La credencial no existe")
        val plano = withContext(Dispatchers.IO) { cifrador.descifrar(entidad.contenidoCifrado, id) }
        try {
            return when (entidad.tipo) {
                CredentialKind.CLAVE -> {
                    val publica = entidad.clavePublica ?: throw CredentialUnavailableException("La clave guardada no tiene parte pública")
                    Credential.PublicKey(ClientKey.desdeAlmacen(publica, plano))
                }

                CredentialKind.CONTRASENA -> {
                    val caracteres = SecretText.caracteres(plano)
                    try {
                        Credential.Password(caracteres)
                    } finally {
                        caracteres.fill('\u0000')
                    }
                }
            }
        } finally {
            plano.fill(0)
        }
    }

    public suspend fun eliminar(id: String) {
        dao.eliminar(id)
    }

    private suspend fun guardarClave(
        nombre: String,
        clave: ClientKey,
    ): CredentialSummary {
        val id = UUID.randomUUID().toString()
        val contenido = withContext(Dispatchers.IO) { cifrador.cifrarYBorrar(clave.pkcs8(), id) }
        val entidad =
            CredentialEntity(
                id = id,
                tipo = CredentialKind.CLAVE,
                nombre = nombre,
                tipoClave = clave.tipo,
                clavePublica = clave.blobPublico,
                huella = clave.huellaSha256(),
                contenidoCifrado = contenido,
                aliasKeystore = cifrador.aliasKeystore,
                creadaEn = reloj(),
            )
        dao.insertar(entidad)
        return resumen(entidad)
    }

    private fun resumen(entidad: CredentialEntity): CredentialSummary =
        CredentialSummary(
            id = entidad.id,
            tipo = entidad.tipo,
            nombre = entidad.nombre,
            tipoClave = entidad.tipoClave,
            huella = entidad.huella,
            lineaPublica =
                entidad.clavePublica?.let { blob ->
                    "${entidad.tipoClave} ${Base64.getEncoder().encodeToString(blob)} ${entidad.nombre}"
                },
            creadaEn = entidad.creadaEn,
        )
}
