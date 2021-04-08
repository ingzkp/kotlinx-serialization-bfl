package com.ing.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

object DoubleSerializer : KSerializer<Double> by (SurrogateSerializer(DoubleSurrogate.serializer()) { DoubleSurrogate.from(it) })

@Suppress("ArrayInDataClass")
@Serializable
data class DoubleSurrogate(
    override val sign: Byte,
    @FixedLength([DOUBLE_INTEGER_SIZE]) override val integer: ByteArray,
    @FixedLength([DOUBLE_FRACTION_SIZE]) override val fraction: ByteArray
) : FloatingPointSurrogate<Double> {
    override fun toOriginal() = toBigDecimal().toDouble()

    companion object {
        const val DOUBLE_INTEGER_SIZE: Int = 309
        const val DOUBLE_FRACTION_SIZE: Int = 325

        // TODO: introduce constants for these magic numbers, also in PrimitiveElement.kt, tests and any other places they occur.
        const val DOUBLE_SIZE = 1 + (4 + DOUBLE_INTEGER_SIZE) + (4 + DOUBLE_FRACTION_SIZE)

        fun from(double: Double): DoubleSurrogate {
            val (sign, integer, fraction) = double.toBigDecimal().asByteTriple()
            return DoubleSurrogate(sign, integer, fraction)
        }
    }
}
