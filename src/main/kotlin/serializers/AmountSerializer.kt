package serializers

import annotations.FixedLength
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.corda.core.contracts.Amount
import java.math.BigDecimal
import java.security.MessageDigest

@ExperimentalSerializationApi
object AmountStringSerializer : KSerializer<Amount<String>> {
    private val strategy = AmountStringSurrogate.serializer()
    override val descriptor: SerialDescriptor = strategy.descriptor

    override fun deserialize(decoder: Decoder): Amount<String> {
        return decoder.decodeSerializableValue(strategy).toOriginal()
    }

    override fun serialize(encoder: Encoder, value: Amount<String>) {
        encoder.encodeSerializableValue(
            strategy,
            AmountStringSurrogate.from(value)
        )
    }
}

@Suppress("ArrayInDataClass")
@ExperimentalSerializationApi
@Serializable
data class AmountStringSurrogate(
    val quantity: Long,
    @Serializable(BigDecimalSerializer::class)
    val displayTokenSize: BigDecimal,
    @FixedLength([32])
    val token: ByteArray
) {
    companion object {
        fun from(original: Amount<String>): AmountStringSurrogate {
            val messageDigest = MessageDigest.getInstance("SHA-256")
            messageDigest.update(original.token.toByteArray())
            val tokenNameHash = messageDigest.digest()
            return AmountStringSurrogate(original.quantity, original.displayTokenSize, tokenNameHash)
        }
    }

    fun toOriginal(): Amount<String> = Amount(this.quantity, this.displayTokenSize, "some.token")
}
