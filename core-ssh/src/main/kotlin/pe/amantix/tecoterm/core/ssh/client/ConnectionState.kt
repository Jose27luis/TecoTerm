package pe.amantix.tecoterm.core.ssh.client

import pe.amantix.tecoterm.core.ssh.SshException

public sealed interface ConnectionState {
    public data object Conectada : ConnectionState

    public data class Desconectada(
        public val causa: SshException?,
    ) : ConnectionState
}
