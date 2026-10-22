package pe.amantix.tecoterm.core.security

public class CredentialUnavailableException(
    mensaje: String,
    causa: Throwable? = null,
) : Exception(mensaje, causa)
