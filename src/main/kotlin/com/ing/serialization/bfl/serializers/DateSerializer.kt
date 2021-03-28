package com.ing.serialization.bfl.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.Date

object DateSerializer : KSerializer<Date> {
    private val strategy = DateSurrogate.serializer()
    override val descriptor: SerialDescriptor = strategy.descriptor

    override fun deserialize(decoder: Decoder): Date {
        val surrogate = decoder.decodeSerializableValue(strategy)
        return Date(surrogate.l)
    }

    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeSerializableValue(strategy, DateSurrogate(value.time))
    }
}

@Serializable
data class DateSurrogate(val l: Long)
