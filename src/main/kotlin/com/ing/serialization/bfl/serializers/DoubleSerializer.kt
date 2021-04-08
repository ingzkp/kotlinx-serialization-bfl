package com.ing.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DoubleSerializer : KSerializer<Double> {
    private val strategy = DoubleSurrogate.serializer()
    override val descriptor: SerialDescriptor = strategy.descriptor
    override fun serialize(encoder: Encoder, value: Double) {
        encoder.encodeSerializableValue(strategy, DoubleSurrogate.from(value))
    }

    override fun deserialize(decoder: Decoder): Double {
        val surrogate = decoder.decodeSerializableValue(strategy)
        return surrogate.toOriginal()
    }
}

@Suppress("ArrayInDataClass")
@Serializable
data class DoubleSurrogate(
    override val sign: Byte,
    @FixedLength([DOUBLE_INTEGER_SIZE]) override val integer: ByteArray,
    @FixedLength([DOUBLE_FRACTION_SIZE]) override val fraction: ByteArray
) : FloatingPointSurrogate {
    fun toOriginal() = toBigDecimal().toDouble()

    companion object {
        const val DOUBLE_INTEGER_SIZE: Int = 309
        const val DOUBLE_FRACTION_SIZE: Int = 325

        // TODO: @Victor: What are these magic numbers? Let's make constants for them somewhere
        const val DOUBLE_SIZE = 1 + (4 + DOUBLE_INTEGER_SIZE) + (4 + DOUBLE_FRACTION_SIZE)

        fun from(double: Double): DoubleSurrogate {
            val (sign, integer, fraction) = double.toBigDecimal().asByteTriple()
            return DoubleSurrogate(sign, integer, fraction)
        }
    }
}
