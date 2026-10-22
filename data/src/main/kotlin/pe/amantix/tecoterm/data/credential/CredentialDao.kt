package pe.amantix.tecoterm.data.credential

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface CredentialDao {
    @Query("SELECT * FROM credenciales ORDER BY nombre COLLATE NOCASE")
    fun observar(): Flow<List<CredentialEntity>>

    @Query("SELECT * FROM credenciales WHERE id = :id")
    suspend fun obtener(id: String): CredentialEntity?

    @Insert
    suspend fun insertar(credencial: CredentialEntity)

    @Query("DELETE FROM credenciales WHERE id = :id")
    suspend fun eliminar(id: String)
}
