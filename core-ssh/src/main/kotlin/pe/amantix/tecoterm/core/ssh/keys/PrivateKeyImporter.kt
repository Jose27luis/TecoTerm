package pe.amantix.tecoterm.core.ssh.keys

public object PrivateKeyImporter {
    public fun importar(
        texto: String,
        passphrase: CharArray?,
    ): ImportedKey {
        try {
            val bloque = PemArmor.leer(texto)
            return if (bloque.tipo == OpenSshKeyParser.TIPO_PEM) {
                OpenSshKeyParser.leer(bloque.contenido, passphrase)
            } else {
                PemKeyParser.leer(bloque, passphrase)
            }
        } finally {
            passphrase?.fill('\u0000')
        }
    }
}
