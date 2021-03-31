package com.ing.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.ZoneId
import java.time.ZonedDateTime

object ZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
    private val strategy = ZonedDateTimeSurrogate.serializer()
    override val descriptor: SerialDescriptor = strategy.descriptor

    override fun deserialize(decoder: Decoder): ZonedDateTime {
        return decoder.decodeSerializableValue(strategy).toOriginal()
    }

    override fun serialize(encoder: Encoder, value: ZonedDateTime) {
        encoder.encodeSerializableValue(strategy, ZonedDateTimeSurrogate.from(value))
    }
}

@Serializable
data class ZonedDateTimeSurrogate(
    val year: Int,
    val month: Int,
    val dayOfMonth: Int,
    val hour: Int,
    val minute: Int,
    val second: Int,
    val nanoOfSecond: Int,
    @FixedLength([20])
    val zone: String
) {

    fun toOriginal(): ZonedDateTime =
        ZonedDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond, ZoneId.of(zone))

    companion object {
        fun from(original: ZonedDateTime) = with(original) {
            ZonedDateTimeSurrogate(year, month.value, dayOfMonth, hour, minute, second, nano, zone.id)
        }
    }
}
