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
        fun from(bigDecimal: BigDecimal): BigDecimalSurrogate {
            val (integerPart, fractionalPart) = representOrThrow(bigDecimal)

            val sign = bigDecimal.signum().toByte()

            val integer = integerPart.toListOfDecimals()

            val fraction = (fractionalPart?.toListOfDecimals() ?: emptyByteArray())

            return BigDecimalSurrogate(sign, integer, fraction)
        }

        private fun emptyByteArray(): ByteArray = ByteArray(0) { 0 }
        private fun String.toListOfDecimals(): ByteArray {
            return this.map {
                // Experimental: prefer plain java version.
                // it.digitToInt()
                Character.getNumericValue(it).toByte()
            }.toByteArray()
        }

        private fun representOrThrow(bigDecimal: BigDecimal): Pair<String, String?> {
            val integerFractionPair = bigDecimal.toPlainString().removePrefix("-").split(".")

            val integerPart = integerFractionPair[0]
            val fractionalPart = integerFractionPair.getOrNull(1)

            return Pair(integerPart, fractionalPart)
        }
    }
}
