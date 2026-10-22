package pe.amantix.tecoterm.data.knownhost

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import pe.amantix.tecoterm.core.ssh.crypto.ClientKey
import pe.amantix.tecoterm.core.ssh.crypto.HostKey
import java.security.KeyPairGenerator

internal class KnownHostVerifierTest {
    private val dao = MemoryKnownHostDao()
    private val prompter = RecordingPrompter()
    private val verificador = KnownHostVerifier(dao, prompter, reloj = { 1_000L })

    @Test
    fun `un servidor nuevo aceptado se recuerda`() =
        runBlocking {
            prompter.acepta = true
            val clave = claveNueva()

            assertTrue(verificador.verificar("vps.pe", 22, clave))
            assertEquals(HostKeyPrompt("vps.pe", 22, "ssh-ed25519", clave.huellaSha256()), prompter.preguntas.single())
            assertEquals(clave.huellaSha256(), dao.obtener("vps.pe", 22)?.huella)
        }

    @Test
    fun `un servidor nuevo rechazado no se recuerda`() =
        runBlocking {
            prompter.acepta = false

            assertFalse(verificador.verificar("vps.pe", 22, claveNueva()))
            assertNull(dao.obtener("vps.pe", 22))
        }

    @Test
    fun `un servidor conocido con la misma clave entra sin preguntar`() =
        runBlocking {
            prompter.acepta = true
            val clave = claveNueva()
            verificador.verificar("vps.pe", 22, clave)
            prompter.preguntas.clear()

            assertTrue(verificador.verificar("vps.pe", 22, clave))
            assertTrue(prompter.preguntas.isEmpty())
        }

    @Test
    fun `una clave distinta se rechaza siempre y se avisa`() =
        runBlocking {
            prompter.acepta = true
            val original = claveNueva()
            val nueva = claveNueva()
            verificador.verificar("vps.pe", 22, original)

            assertFalse(verificador.verificar("vps.pe", 22, nueva))
            val cambio = prompter.cambios.single()
            assertEquals(original.huellaSha256(), cambio.huellaAnterior)
            assertEquals(nueva.huellaSha256(), cambio.huellaNueva)
            assertEquals(original.huellaSha256(), dao.obtener("vps.pe", 22)?.huella)
        }

    @Test
    fun `el mismo host en otro puerto es otro servidor`() =
        runBlocking {
            prompter.acepta = true
            verificador.verificar("vps.pe", 22, claveNueva())

            assertTrue(verificador.verificar("vps.pe", 2222, claveNueva()))
            assertEquals(2, prompter.preguntas.size)
        }

    private fun claveNueva(): HostKey =
        HostKey.desdeBlob(ClientKey.desde(KeyPairGenerator.getInstance("Ed25519").generateKeyPair()).blobPublico)

    private class RecordingPrompter : HostKeyPrompter {
        var acepta: Boolean = false
        val preguntas: MutableList<HostKeyPrompt> = mutableListOf()
        val cambios: MutableList<HostKeyChange> = mutableListOf()

        override suspend fun confirmarNuevo(servidor: HostKeyPrompt): Boolean {
            preguntas += servidor
            return acepta
        }

        override suspend fun avisarCambio(cambio: HostKeyChange) {
            cambios += cambio
        }
    }

    private class MemoryKnownHostDao : KnownHostDao {
        private val filas = MutableStateFlow<Map<Pair<String, Int>, KnownHostEntity>>(emptyMap())

        override fun observar(): Flow<List<KnownHostEntity>> = MutableStateFlow(filas.value.values.toList())

        override suspend fun obtener(
            host: String,
            puerto: Int,
        ): KnownHostEntity? = filas.value[host to puerto]

        override suspend fun guardar(servidor: KnownHostEntity) {
            filas.value += (servidor.host to servidor.puerto) to servidor
        }

        override suspend fun eliminar(
            host: String,
            puerto: Int,
        ) {
            filas.value -= host to puerto
        }
    }
}
