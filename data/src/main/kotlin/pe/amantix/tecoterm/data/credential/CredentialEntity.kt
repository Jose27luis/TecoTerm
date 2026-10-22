package pe.amantix.tecoterm.data.credential

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credenciales")
internal data class CredentialEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "tipo")
    val tipo: CredentialKind,
    @ColumnInfo(name = "nombre")
    val nombre: String,
    @ColumnInfo(name = "tipo_clave")
    val tipoClave: String?,
    @ColumnInfo(name = "clave_publica")
    val clavePublica: ByteArray?,
    @ColumnInfo(name = "huella")
    val huella: String?,
    @ColumnInfo(name = "contenido_cifrado")
    val contenidoCifrado: ByteArray,
    @ColumnInfo(name = "alias_keystore")
    val aliasKeystore: String,
    @ColumnInfo(name = "creada_en")
    val creadaEn: Long,
)
