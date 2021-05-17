package com.ing.serialization.bfl.serde.serializers.custom.polymorphic

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import sun.security.provider.DSAPublicKeyImpl
import sun.security.rsa.RSAPublicKeyImpl
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec

object RSAPublicKeySerializer :
    SurrogateSerializer<RSAPublicKeyImpl, RSASurrogate>(RSASurrogate.serializer(), { RSASurrogate(it.encoded) })

object DSAPublicKeySerializer :
    SurrogateSerializer<DSAPublicKeyImpl, DSASurrogate>(DSASurrogate.serializer(), { DSASurrogate(it.encoded) })

@Suppress("ArrayInDataClass")
@Serializable
@SerialName("RSA")
data class RSASurrogate(
    @FixedLength([ENCODED_SIZE])
    val encoded: ByteArray
) : Surrogate<RSAPublicKeyImpl> {
    override fun toOriginal(): RSAPublicKeyImpl =
        KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(encoded)) as RSAPublicKeyImpl

    companion object {
        const val ENCODED_SIZE = 900
        const val SERIAL_NAME_LENGTH = 2 + 2 * 3
    }
}

@Suppress("ArrayInDataClass")
@Serializable
@SerialName("DSA")
data class DSASurrogate(
    @FixedLength([ENCODED_SIZE])
    val encoded: ByteArray
) : Surrogate<DSAPublicKeyImpl> {
    override fun toOriginal(): DSAPublicKeyImpl =
        KeyFactory.getInstance("DSA").generatePublic(X509EncodedKeySpec(encoded)) as DSAPublicKeyImpl

    companion object {
        const val ENCODED_SIZE = 900
        const val SERIAL_NAME_LENGTH = 2 + 2 * 3
    }
}
