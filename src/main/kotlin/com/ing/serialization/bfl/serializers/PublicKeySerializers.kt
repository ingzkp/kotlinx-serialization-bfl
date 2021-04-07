package com.ing.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.serialization.bfl.api.Surrogate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import sun.security.provider.DSAPublicKeyImpl
import sun.security.rsa.RSAPublicKeyImpl
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec

abstract class PublicKeyBaseSurrogate {
    @FixedLength([encodedSize])
    abstract val encoded: ByteArray

    companion object {
        const val encodedSize = 900
    }
}

object RSAPublicKeySerializer : KSerializer<RSAPublicKeyImpl>
by (SurrogateSerializer(RSASurrogate.serializer()) { RSASurrogate(it.encoded) })

object DSAPublicKeySerializer : KSerializer<DSAPublicKeyImpl>
by (SurrogateSerializer(DSASurrogate.serializer()) { DSASurrogate(it.encoded) })

@Suppress("ArrayInDataClass")
@Serializable
data class RSASurrogate(
    @FixedLength([encodedSize])
    override val encoded: ByteArray
) : Surrogate<RSAPublicKeyImpl>, PublicKeyBaseSurrogate() {
    override fun toOriginal(): RSAPublicKeyImpl =
        KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(encoded)) as RSAPublicKeyImpl
}

@Suppress("ArrayInDataClass")
@Serializable
data class DSASurrogate(
    @FixedLength([encodedSize])
    override val encoded: ByteArray
) : Surrogate<DSAPublicKeyImpl>, PublicKeyBaseSurrogate() {
    override fun toOriginal(): DSAPublicKeyImpl =
        KeyFactory.getInstance("DSA").generatePublic(X509EncodedKeySpec(encoded)) as DSAPublicKeyImpl
}
