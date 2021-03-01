package serializers

import RSAPublicKeySurrogate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import sun.security.rsa.RSAPublicKeyImpl

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