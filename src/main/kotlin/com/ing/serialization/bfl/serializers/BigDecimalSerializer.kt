package com.ing.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
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
    @FixedLength([INTEGER_SIZE]) val integer: ByteArray,
    @FixedLength([FRACTION_SIZE]) val fraction: ByteArray
) {
    init {
        require(integer.size == INTEGER_SIZE) {
            "integer part must have size $INTEGER_SIZE, but has ${integer.size}"
        }
        require(fraction.size == FRACTION_SIZE) {
            "fraction part must have size $FRACTION_SIZE, but has ${fraction.size}"
        }
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
        const val INTEGER_SIZE: Int = 100
        const val FRACTION_SIZE: Int = 20
        const val SIZE = 1 + (4 + INTEGER_SIZE) + (4 + FRACTION_SIZE)

        val MAX = BigDecimalSurrogate(1, ByteArray(INTEGER_SIZE) { 9 }, ByteArray(FRACTION_SIZE) { 9 }).toOriginal()

        fun from(bigDecimal: BigDecimal): BigDecimalSurrogate {
            val (integerPart, fractionalPart) = representOrThrow(bigDecimal)

            val sign = bigDecimal.signum().toByte()

            val integer = ByteArray(INTEGER_SIZE - integerPart.length) { 0 } +
                integerPart.toListOfDecimals()

            val fraction = (fractionalPart?.toListOfDecimals() ?: emptyByteArray()) +
                ByteArray(FRACTION_SIZE - (fractionalPart?.length ?: 0)) { 0 }

            val bigDecimalSurrogate = BigDecimalSurrogate(sign, integer, fraction)
            println("surrogate: $bigDecimalSurrogate")
            return bigDecimalSurrogate
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

            require(integerPart.length <= INTEGER_SIZE && (fractionalPart?.length ?: 0) <= FRACTION_SIZE) {
                "Zinc supports only $INTEGER_SIZE digits in integer part and $FRACTION_SIZE digits in fraction part"
            }

            return Pair(integerPart, fractionalPart)
        }
    }
}
