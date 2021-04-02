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
    val sign: Byte,
    val integer: ByteArray,
    val fraction: ByteArray
) {
    init {
//        require(integer.size == INTEGER_SIZE) {
//            "integer part must have size $INTEGER_SIZE, but has ${integer.size}"
//        }
//        require(fraction.size == FRACTION_SIZE) {
//            "fraction part must have size $FRACTION_SIZE, but has ${fraction.size}"
//        }
    }

    fun toOriginal(): BigDecimal {
        val integer = this.integer.joinToString(separator = "") { "$it" }.trimStart('0')
        val fraction = this.fraction.joinToString(separator = "") { "$it" }.trimEnd('0')
        var digit = if (fraction.isEmpty()) integer else "$integer.$fraction"
        if (this.sign == (-1).toByte()) {
            digit = "-$digit"
        }
        return BigDecimal(digit)
    }

    companion object {
        const val DOUBLE_INTEGER_SIZE: Int = 100
        const val DOUBLE_FRACTION_SIZE: Int = 20
        const val DOUBLE_SIZE = 1 + (4 + DOUBLE_INTEGER_SIZE) + (4 + DOUBLE_FRACTION_SIZE)

        fun from(bigDecimal: BigDecimal): BigDecimalSurrogate {
            val (integerPart, fractionalPart) = representOrThrow(bigDecimal)

            val sign = bigDecimal.signum().toByte()

            val integer = integerPart.toListOfDecimals()

            val fraction = (fractionalPart?.toListOfDecimals() ?: emptyByteArray())

            return BigDecimalSurrogate(sign, integer, fraction)
        }

        fun fromDouble(bigDecimal: BigDecimal): BigDecimalSurrogate {
            val (integerPart, fractionalPart) = representOrThrow(bigDecimal)

            val sign = bigDecimal.signum().toByte()

            val integer = ByteArray(DOUBLE_INTEGER_SIZE - integerPart.length) { 0 } +
                integerPart.toListOfDecimals()

            val fraction = (fractionalPart?.toListOfDecimals() ?: ByteArray(0)) +
                ByteArray(DOUBLE_FRACTION_SIZE - (fractionalPart?.length ?: 0)) { 0 }

            return BigDecimalSurrogate(sign, integer, fraction)
        }

        private fun String.toListOfDecimals() = map {
            // Experimental: prefer plain java version.
            // it.digitToInt()
            Character.getNumericValue(it).toByte()
        }.toByteArray()

        internal fun from(float: Float) =
            try {
                fromDouble(float.toBigDecimal())
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Float is too large for BigDecimalSurrogate", e)
            }

        internal fun from(double: Double) =
            try {
                fromDouble(double.toBigDecimal())
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Double is too large for BigDecimalSurrogate", e)
            }

        private fun representOrThrow(bigDecimal: BigDecimal): Pair<String, String?> {
            val integerFractionPair = bigDecimal.toPlainString().removePrefix("-").split(".")

            val integerPart = integerFractionPair[0]
            val fractionalPart = integerFractionPair.getOrNull(1)

            return Pair(integerPart, fractionalPart)
        }
    }
}
