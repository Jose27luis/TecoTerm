package pe.amantix.tecoterm.core.ssh.keys

import pe.amantix.tecoterm.core.ssh.crypto.ClientKey

public class ImportedKey internal constructor(
    public val clave: ClientKey,
    public val comentario: String,
)
