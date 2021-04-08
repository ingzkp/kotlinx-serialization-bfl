package com.ing.serialization.bfl.serde.serializers.custom.polymorphic

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import sun.security.provider.DSAPublicKeyImpl
import sun.security.rsa.RSAPublicKeyImpl
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec

abstract class PublicKeyBaseSurrogate {
    @FixedLength([ENCODED_SIZE])
    abstract val encoded: ByteArray

    companion object {
        const val ENCODED_SIZE = 900
    }
}

object RSAPublicKeySerializer : KSerializer<RSAPublicKeyImpl>
by (SurrogateSerializer(RSASurrogate.serializer()) { RSASurrogate(it.encoded) })

object DSAPublicKeySerializer : KSerializer<DSAPublicKeyImpl>
by (SurrogateSerializer(DSASurrogate.serializer()) { DSASurrogate(it.encoded) })

@Suppress("ArrayInDataClass")
@Serializable
data class RSASurrogate(
    @FixedLength([ENCODED_SIZE])
    override val encoded: ByteArray
) : Surrogate<RSAPublicKeyImpl>, PublicKeyBaseSurrogate() {
    override fun toOriginal(): RSAPublicKeyImpl =
        KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(encoded)) as RSAPublicKeyImpl
}

@Suppress("ArrayInDataClass")
@Serializable
data class DSASurrogate(
    @FixedLength([ENCODED_SIZE])
    override val encoded: ByteArray
) : Surrogate<DSAPublicKeyImpl>, PublicKeyBaseSurrogate() {
    override fun toOriginal(): DSAPublicKeyImpl =
        KeyFactory.getInstance("DSA").generatePublic(X509EncodedKeySpec(encoded)) as DSAPublicKeyImpl
}
