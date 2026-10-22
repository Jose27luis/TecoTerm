package pe.amantix.tecoterm.core.ssh

public sealed class SshException(
    mensaje: String,
    causa: Throwable? = null,
) : Exception(mensaje, causa)

public class HostNotFoundException(
    public val host: String,
    causa: Throwable,
) : SshException("No se encontró el host $host", causa)

public class ConnectionRefusedException(
    public val host: String,
    public val puerto: Int,
    causa: Throwable,
) : SshException("El servidor $host:$puerto rechazó la conexión", causa)

public class ConnectionTimeoutException(
    mensaje: String,
    causa: Throwable? = null,
) : SshException(mensaje, causa)

public class HostKeyRejectedException(
    public val huella: String,
) : SshException("La clave del servidor $huella no fue aceptada")

public class AuthenticationFailedException(
    public val metodosRestantes: List<String>,
) : SshException("El servidor rechazó la autenticación")

public class SshProtocolException(
    mensaje: String,
    causa: Throwable? = null,
) : SshException(mensaje, causa)

public class ServerDisconnectedException(
    public val codigo: Int,
    public val descripcion: String,
) : SshException("El servidor cerró la conexión ($codigo): $descripcion")

public class ConnectionLostException(
    mensaje: String,
    causa: Throwable? = null,
) : SshException(mensaje, causa)

public class ConnectionClosedException : SshException("La conexión ya está cerrada")

public class ChannelOpenFailedException(
    public val codigo: Int,
    public val descripcion: String,
) : SshException("El servidor no abrió el canal ($codigo): $descripcion")

public class ChannelRequestFailedException(
    public val peticion: String,
) : SshException("El servidor rechazó la petición $peticion")
