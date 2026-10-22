package pe.amantix.tecoterm.core.ssh.auth

import pe.amantix.tecoterm.core.ssh.crypto.ClientKey

public sealed interface Credential {
    public class Password(
        contrasena: CharArray,
    ) : Credential {
        internal val contrasena: CharArray = contrasena.copyOf()

        public fun borrar() {
            contrasena.fill('\u0000')
        }
    }

    public class PublicKey(
        public val clave: ClientKey,
    ) : Credential

    public data object KeyboardInteractive : Credential
}
