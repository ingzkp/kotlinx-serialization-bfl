package serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.Date

// Serializer: Implement KSerializer for 3rd party classes.

@Serializable
data class DateSurrogate(val l: Long)

object DateSerializer : KSerializer<Date> {
    private val strategy = DateSurrogate.serializer()
    override val descriptor: SerialDescriptor = strategy.descriptor
    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeSerializableValue(strategy, DateSurrogate(value.time))
    }

    override fun deserialize(decoder: Decoder): Date {
        val surrogate = decoder.decodeSerializableValue(strategy)
        return Date(surrogate.l)
    }
}
