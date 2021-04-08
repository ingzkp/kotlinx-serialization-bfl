package com.ing.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal

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
    val sign: Byte,
    @FixedLength([DOUBLE_INTEGER_SIZE]) val integer: ByteArray,
    @FixedLength([DOUBLE_FRACTION_SIZE]) val fraction: ByteArray
) {
    fun toOriginal(): Double {
        val integer = this.integer.joinToString(separator = "") { "$it" }.trimStart('0')
        val fraction = this.fraction.joinToString(separator = "") { "$it" }.trimEnd('0')
        var digit = if (fraction.isEmpty()) integer else "$integer.$fraction"
        if (this.sign == (-1).toByte()) {
            digit = "-$digit"
        }
        return BigDecimal(digit).toDouble()
    }

    companion object {
        const val DOUBLE_INTEGER_SIZE: Int = 309
        const val DOUBLE_FRACTION_SIZE: Int = 325

        // TODO: @Victor: What are these magic numbers? Let's make constants for them somewhere
        const val DOUBLE_SIZE = 1 + (4 + DOUBLE_INTEGER_SIZE) + (4 + DOUBLE_FRACTION_SIZE)

        fun from(double: Double): DoubleSurrogate {
            val doubleAsBigDecimal = double.toBigDecimal()
            val (integerPart, fractionalPart) = representOrThrow(doubleAsBigDecimal)

            val sign = doubleAsBigDecimal.signum().toByte()
            val integer = integerPart.toListOfDecimals()
            val fraction = (fractionalPart?.toListOfDecimals() ?: ByteArray(0))

            return DoubleSurrogate(sign, integer, fraction)
        }

        private fun String.toListOfDecimals(): ByteArray {
            return this.map {
                // Experimental: prefer plain java version.
                // it.digitToInt()
                Character.getNumericValue(it).toByte()
            }.toByteArray()
        }

        private fun representOrThrow(bigDecimal: BigDecimal): Pair<String, String?> {
            val integerFractionPair = bigDecimal.toPlainString().removePrefix("-").split(".")

            val integerPart = integerFractionPair.getOrNull(0)
                ?: error("Cannot convert BigDecimal ${bigDecimal.toPlainString()} to its integer and fractional parts")
            val fractionalPart = integerFractionPair.getOrNull(1)

            return Pair(integerPart, fractionalPart)
        }
    }
}
