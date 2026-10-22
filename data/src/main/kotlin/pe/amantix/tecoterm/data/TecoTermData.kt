package pe.amantix.tecoterm.data

import android.content.Context
import androidx.room.Room
import pe.amantix.tecoterm.core.security.CredentialCipher
import pe.amantix.tecoterm.data.credential.CredentialRepository
import pe.amantix.tecoterm.data.knownhost.KnownHostRepository

public class TecoTermData(
    contexto: Context,
) {
    private val baseDatos: TecoTermDatabase =
        Room.databaseBuilder(contexto.applicationContext, TecoTermDatabase::class.java, NOMBRE_BASE).build()

    public val credenciales: CredentialRepository =
        CredentialRepository(baseDatos.credenciales(), CredentialCipher(), System::currentTimeMillis)

    public val servidoresConocidos: KnownHostRepository =
        KnownHostRepository(baseDatos.servidoresConocidos(), System::currentTimeMillis)

    private companion object {
        const val NOMBRE_BASE: String = "tecoterm.db"
    }
}
