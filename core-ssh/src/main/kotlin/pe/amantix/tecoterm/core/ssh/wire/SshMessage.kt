package pe.amantix.tecoterm.core.ssh.wire

internal object SshMessage {
    const val SSH_MSG_DISCONNECT: Int = 1
    const val SSH_MSG_IGNORE: Int = 2
    const val SSH_MSG_UNIMPLEMENTED: Int = 3
    const val SSH_MSG_DEBUG: Int = 4
    const val SSH_MSG_SERVICE_REQUEST: Int = 5
    const val SSH_MSG_SERVICE_ACCEPT: Int = 6
    const val SSH_MSG_EXT_INFO: Int = 7
    const val SSH_MSG_KEXINIT: Int = 20
    const val SSH_MSG_NEWKEYS: Int = 21
    const val SSH_MSG_KEX_ECDH_INIT: Int = 30
    const val SSH_MSG_KEX_ECDH_REPLY: Int = 31
    const val SSH_MSG_USERAUTH_REQUEST: Int = 50
    const val SSH_MSG_USERAUTH_FAILURE: Int = 51
    const val SSH_MSG_USERAUTH_SUCCESS: Int = 52
    const val SSH_MSG_USERAUTH_BANNER: Int = 53
    const val SSH_MSG_USERAUTH_PK_OK: Int = 60
    const val SSH_MSG_USERAUTH_INFO_REQUEST: Int = 60
    const val SSH_MSG_USERAUTH_INFO_RESPONSE: Int = 61
    const val SSH_MSG_GLOBAL_REQUEST: Int = 80
    const val SSH_MSG_REQUEST_SUCCESS: Int = 81
    const val SSH_MSG_REQUEST_FAILURE: Int = 82
    const val SSH_MSG_CHANNEL_OPEN: Int = 90
    const val SSH_MSG_CHANNEL_OPEN_CONFIRMATION: Int = 91
    const val SSH_MSG_CHANNEL_OPEN_FAILURE: Int = 92
    const val SSH_MSG_CHANNEL_WINDOW_ADJUST: Int = 93
    const val SSH_MSG_CHANNEL_DATA: Int = 94
    const val SSH_MSG_CHANNEL_EXTENDED_DATA: Int = 95
    const val SSH_MSG_CHANNEL_EOF: Int = 96
    const val SSH_MSG_CHANNEL_CLOSE: Int = 97
    const val SSH_MSG_CHANNEL_REQUEST: Int = 98
    const val SSH_MSG_CHANNEL_SUCCESS: Int = 99
    const val SSH_MSG_CHANNEL_FAILURE: Int = 100

    const val SSH_DISCONNECT_PROTOCOL_ERROR: Int = 2
    const val SSH_DISCONNECT_KEY_EXCHANGE_FAILED: Int = 3
    const val SSH_DISCONNECT_MAC_ERROR: Int = 5
    const val SSH_DISCONNECT_HOST_KEY_NOT_VERIFIABLE: Int = 9
    const val SSH_DISCONNECT_BY_APPLICATION: Int = 11
    const val SSH_DISCONNECT_NO_MORE_AUTH_METHODS_AVAILABLE: Int = 14

    const val SSH_OPEN_ADMINISTRATIVELY_PROHIBITED: Int = 1
    const val SSH_EXTENDED_DATA_STDERR: Int = 1

    fun esDeTransporte(numero: Int): Boolean = numero in SSH_MSG_DISCONNECT..SSH_MSG_KEX_ECDH_REPLY

    fun esDeIntercambio(numero: Int): Boolean = numero in SSH_MSG_KEXINIT..SSH_MSG_KEX_ECDH_REPLY
}
