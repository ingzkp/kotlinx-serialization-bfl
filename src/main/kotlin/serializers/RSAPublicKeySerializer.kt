package serializers

import annotations.FixedLength
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import sun.security.rsa.RSAPublicKeyImpl

@Suppress("ArrayInDataClass")
@Serializable
@SerialName("RSAPublicKeyImpl")
data class RSAPublicKeySurrogate(@FixedLength([500]) val encoded: ByteArray)

object RSAPublicKeySerializer : KSerializer<RSAPublicKeyImpl> {
    private val strategy = RSAPublicKeySurrogate.serializer()
    override val descriptor = strategy.descriptor

    override fun serialize(encoder: Encoder, value: RSAPublicKeyImpl) {
        encoder.encodeSerializableValue(strategy, RSAPublicKeySurrogate(value.encoded))
    }

    override fun deserialize(decoder: Decoder): RSAPublicKeyImpl {
        val surrogate = decoder.decodeSerializableValue(strategy)
        return RSAPublicKeyImpl.newKey(surrogate.encoded) as RSAPublicKeyImpl
    }
}