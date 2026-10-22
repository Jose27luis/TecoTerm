package pe.amantix.tecoterm.core.ssh.crypto

import java.nio.ByteBuffer
import java.nio.CharBuffer

public object SecretText {
    public fun utf8(caracteres: CharArray): ByteArray {
        val buffer = Charsets.UTF_8.encode(CharBuffer.wrap(caracteres))
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        if (buffer.hasArray()) {
            buffer.array().fill(0)
        }
        return bytes
    }

    public fun caracteres(utf8: ByteArray): CharArray {
        val buffer = Charsets.UTF_8.decode(ByteBuffer.wrap(utf8))
        val caracteres = CharArray(buffer.remaining())
        buffer.get(caracteres)
        if (buffer.hasArray()) {
            buffer.array().fill('\u0000')
        }
        return caracteres
    }
}
