package com.ing.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

object FloatSerializer : KSerializer<Float> by (SurrogateSerializer(FloatSurrogate.serializer()) { FloatSurrogate.from(it) })

@Suppress("ArrayInDataClass")
@Serializable
data class FloatSurrogate(
    override val sign: Byte,
    @FixedLength([FLOAT_INTEGER_SIZE]) override val integer: ByteArray,
    @FixedLength([FLOAT_FRACTION_SIZE]) override val fraction: ByteArray
) : FloatingPointSurrogate<Float> {
    override fun toOriginal() = toBigDecimal().toFloat()

    companion object {
        const val FLOAT_INTEGER_SIZE: Int = 39
        const val FLOAT_FRACTION_SIZE: Int = 46

        // TODO: introduce constants for these magic numbers, also in PrimitiveElement.kt, tests and any other places they occur.
        const val FLOAT_SIZE = 1 + (4 + FLOAT_INTEGER_SIZE) + (4 + FLOAT_FRACTION_SIZE)

        fun from(double: Float): FloatSurrogate {
            val (sign, integer, fraction) = double.toBigDecimal().asByteTriple()
            return FloatSurrogate(sign, integer, fraction)
        }
    }
}
