package com.ing.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal

typealias DoubleBigDecimal = BigDecimal

object DoubleSerializer : KSerializer<DoubleBigDecimal> {
    private val strategy = DoubleSurrogate.serializer()
    override val descriptor: SerialDescriptor = strategy.descriptor
    override fun serialize(encoder: Encoder, value: DoubleBigDecimal) {
        encoder.encodeSerializableValue(strategy, DoubleSurrogate.from(value))
    }

    override fun deserialize(decoder: Decoder): DoubleBigDecimal {
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
    fun toOriginal(): BigDecimal {
        val integer = this.integer.joinToString(separator = "") { "$it" }.trimStart('0')
        val fraction = this.fraction.joinToString(separator = "") { "$it" }.trimEnd('0')
        var digit = if (fraction.isEmpty()) integer else "$integer.$fraction"
        if (this.sign == (-1).toByte()) {
            digit = "-$digit"
        }
        return DoubleBigDecimal(digit)
    }

    companion object {
        const val DOUBLE_INTEGER_SIZE: Int = 100
        const val DOUBLE_FRACTION_SIZE: Int = 20
        const val DOUBLE_SIZE = 1 + (4 + DOUBLE_INTEGER_SIZE) + (4 + DOUBLE_FRACTION_SIZE)

        fun from(bigDecimal: DoubleBigDecimal): DoubleSurrogate {
            val (integerPart, fractionalPart) = representOrThrow(bigDecimal)

            val sign = bigDecimal.signum().toByte()

            val integer = integerPart.toListOfDecimals()

            val fraction = (fractionalPart?.toListOfDecimals() ?: emptyByteArray())

            return DoubleSurrogate(sign, integer, fraction)
        }

        private fun emptyByteArray(): ByteArray = ByteArray(0) { 0 }
        private fun String.toListOfDecimals(): ByteArray {
            return this.map {
                // Experimental: prefer plain java version.
                // it.digitToInt()
                Character.getNumericValue(it).toByte()
            }.toByteArray()
        }

        internal fun from(float: Float) =
            try {
                from(float.toBigDecimal())
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Float is too large for BigDecimalSurrogate", e)
            }

        internal fun from(double: Double) =
            try {
                from(double.toBigDecimal())
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Double is too large for BigDecimalSurrogate", e)
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
