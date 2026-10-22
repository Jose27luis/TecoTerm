package pe.amantix.tecoterm.data.knownhost

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface KnownHostDao {
    @Query("SELECT * FROM servidores_conocidos ORDER BY host, puerto")
    fun observar(): Flow<List<KnownHostEntity>>

    @Query("SELECT * FROM servidores_conocidos WHERE host = :host AND puerto = :puerto")
    suspend fun obtener(
        host: String,
        puerto: Int,
    ): KnownHostEntity?

    @Upsert
    suspend fun guardar(servidor: KnownHostEntity)

    @Query("DELETE FROM servidores_conocidos WHERE host = :host AND puerto = :puerto")
    suspend fun eliminar(
        host: String,
        puerto: Int,
    )
}
