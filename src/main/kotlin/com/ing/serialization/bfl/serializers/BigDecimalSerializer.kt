package com.ing.serialization.bfl.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal

object BigDecimalSerializer : KSerializer<BigDecimal> {
    private val strategy = BigDecimalSurrogate.serializer()
    override val descriptor: SerialDescriptor = strategy.descriptor
    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeSerializableValue(strategy, BigDecimalSurrogate.from(value))
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        val surrogate = decoder.decodeSerializableValue(strategy)
        return surrogate.toOriginal()
    }
}

@Suppress("ArrayInDataClass")
@Serializable
data class BigDecimalSurrogate(
    override val sign: Byte,
    override val integer: ByteArray,
    override val fraction: ByteArray
) : FloatingPointSurrogate {
    fun toOriginal() = toBigDecimal()

    companion object {
        fun from(bigDecimal: BigDecimal): BigDecimalSurrogate {
            val (sign, integer, fraction) = bigDecimal.asByteTriple()
            return BigDecimalSurrogate(sign, integer, fraction)
        }
    }
}
