package com.ing.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.BaseSerializer
import com.ing.serialization.bfl.api.Surrogate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import sun.security.provider.DSAPublicKeyImpl
import sun.security.rsa.RSAPublicKeyImpl
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec

object RSAPublicKeySerializer : KSerializer<RSAPublicKeyImpl>
by (BaseSerializer(RSASurrogate.serializer()) { RSASurrogate(it.encoded) })

@Suppress("ArrayInDataClass")
@Serializable
data class RSASurrogate(@FixedLength([294]) val encoded: ByteArray) : Surrogate<RSAPublicKeyImpl> {
    override fun toOriginal(): RSAPublicKeyImpl =
        KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(encoded)) as RSAPublicKeyImpl
}

object DSAPublicKeySerializer : KSerializer<DSAPublicKeyImpl>
by (BaseSerializer(DSASurrogate.serializer()) { DSASurrogate(it.encoded) })

@Suppress("ArrayInDataClass")
@Serializable
data class DSASurrogate(@FixedLength([500]) val encoded: ByteArray) : Surrogate<DSAPublicKeyImpl> {
    override fun toOriginal(): DSAPublicKeyImpl =
        KeyFactory.getInstance("DSA").generatePublic(X509EncodedKeySpec(encoded)) as DSAPublicKeyImpl
}
