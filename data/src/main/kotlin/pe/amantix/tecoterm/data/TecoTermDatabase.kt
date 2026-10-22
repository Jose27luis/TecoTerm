package pe.amantix.tecoterm.data

import androidx.room.Database
import androidx.room.RoomDatabase
import pe.amantix.tecoterm.data.credential.CredentialDao
import pe.amantix.tecoterm.data.credential.CredentialEntity
import pe.amantix.tecoterm.data.knownhost.KnownHostDao
import pe.amantix.tecoterm.data.knownhost.KnownHostEntity

@Database(
    entities = [CredentialEntity::class, KnownHostEntity::class],
    version = 1,
    exportSchema = true,
)
internal abstract class TecoTermDatabase : RoomDatabase() {
    abstract fun credenciales(): CredentialDao

    abstract fun servidoresConocidos(): KnownHostDao
}
