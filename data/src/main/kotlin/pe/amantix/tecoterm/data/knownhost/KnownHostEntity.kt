package pe.amantix.tecoterm.data.knownhost

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "servidores_conocidos", primaryKeys = ["host", "puerto"])
internal class KnownHostEntity(
    @ColumnInfo(name = "host")
    val host: String,
    @ColumnInfo(name = "puerto")
    val puerto: Int,
    @ColumnInfo(name = "tipo_clave")
    val tipoClave: String,
    @ColumnInfo(name = "clave")
    val clave: ByteArray,
    @ColumnInfo(name = "huella")
    val huella: String,
    @ColumnInfo(name = "visto_por_primera_vez")
    val vistoPorPrimeraVez: Long,
)
