package pe.amantix.tecoterm.core.ssh.crypto

import pe.amantix.tecoterm.core.ssh.SshProtocolException
import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.interfaces.ECPublicKey
import java.security.spec.ECFieldFp
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec

internal object EcP256 {
    const val NOMBRE_CURVA: String = "nistp256"
    const val LONGITUD_COORDENADA: Int = 32
    private const val PUNTO_SIN_COMPRIMIR: Byte = 0x04
    private const val LONGITUD_PUNTO: Int = 1 + 2 * LONGITUD_COORDENADA
    private const val EXPONENTE_CUADRADO: Long = 2
    private const val EXPONENTE_CUBO: Long = 3

    val parametros: ECParameterSpec by lazy {
        val algoritmo = AlgorithmParameters.getInstance("EC")
        algoritmo.init(ECGenParameterSpec("secp256r1"))
        algoritmo.getParameterSpec(ECParameterSpec::class.java)
    }

    fun puntoDesdeBytes(bytes: ByteArray): ECPoint {
        if (bytes.size != LONGITUD_PUNTO || bytes[0] != PUNTO_SIN_COMPRIMIR) {
            throw SshProtocolException("Punto P-256 con formato inválido")
        }
        val punto =
            ECPoint(
                BigInteger(1, bytes.copyOfRange(1, 1 + LONGITUD_COORDENADA)),
                BigInteger(1, bytes.copyOfRange(1 + LONGITUD_COORDENADA, LONGITUD_PUNTO)),
            )
        if (!estaEnLaCurva(punto)) {
            throw SshProtocolException("El punto no pertenece a la curva P-256")
        }
        return punto
    }

    fun bytesDesdePunto(punto: ECPoint): ByteArray =
        byteArrayOf(PUNTO_SIN_COMPRIMIR) +
            coordenada(punto.affineX) +
            coordenada(punto.affineY)

    fun clavePublica(punto: ECPoint): ECPublicKey {
        val clave =
            try {
                KeyFactory.getInstance("EC").generatePublic(ECPublicKeySpec(punto, parametros))
            } catch (error: GeneralSecurityException) {
                throw SshProtocolException("Clave P-256 inválida", error)
            }
        return clave as? ECPublicKey ?: throw SshProtocolException("La plataforma no devolvió una clave EC")
    }

    fun esP256(clave: ECPublicKey): Boolean {
        val propios = clave.params
        return propios.curve == parametros.curve &&
            propios.generator == parametros.generator &&
            propios.order == parametros.order
    }

    private fun estaEnLaCurva(punto: ECPoint): Boolean {
        val curva = parametros.curve
        val primo = (curva.field as? ECFieldFp)?.p ?: return false
        val x = punto.affineX
        val y = punto.affineY
        return enRango(x, primo) &&
            enRango(y, primo) &&
            y.modPow(BigInteger.valueOf(EXPONENTE_CUADRADO), primo) ==
            x
                .modPow(BigInteger.valueOf(EXPONENTE_CUBO), primo)
                .add(curva.a.multiply(x))
                .add(curva.b)
                .mod(primo)
    }

    private fun enRango(
        valor: BigInteger,
        primo: BigInteger,
    ): Boolean = valor.signum() >= 0 && valor < primo

    private fun coordenada(valor: BigInteger): ByteArray {
        val bytes = valor.toByteArray()
        return when {
            bytes.size == LONGITUD_COORDENADA -> bytes
            bytes.size > LONGITUD_COORDENADA -> bytes.copyOfRange(bytes.size - LONGITUD_COORDENADA, bytes.size)
            else -> ByteArray(LONGITUD_COORDENADA - bytes.size) + bytes
        }
    }
}
