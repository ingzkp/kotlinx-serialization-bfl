package serializers

import annotations.FixedLength
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal

const val INTEGER_SIZE: Int = 100
const val FRACTION_SIZE: Int = 20

@ExperimentalSerializationApi
object BigDecimalSerializer: KSerializer<BigDecimal> {
    private val strategy = BigDecimalSurrogate.serializer()
    override val descriptor: SerialDescriptor = strategy.descriptor
    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeSerializableValue(strategy, value.toSurrogate())
    }
    override fun deserialize(decoder: Decoder): BigDecimal {
        val surrogate = decoder.decodeSerializableValue(strategy)
        return surrogate.toOriginal()
    }

    private fun BigDecimal.toSurrogate(): BigDecimalSurrogate {
        val integerFractionPair = this.toPlainString().removePrefix("-").split(".")

        check(fits(integerFractionPair)) {
            "Zinc supports only $INTEGER_SIZE digits in integer part and $FRACTION_SIZE digits in fraction part"
        }

        val paddedIntegerOffset = INTEGER_SIZE - integerFractionPair[0].length

        val sign = this.signum().toByte()
        val integer = copyStringToDigits(integerFractionPair[0], paddedIntegerOffset)

        val fraction
                = if (hasFraction(integerFractionPair)) copyStringToDigits(integerFractionPair[1], 0)
        else ByteArray(FRACTION_SIZE)

        return BigDecimalSurrogate(sign, integer, fraction)
    }

    private fun fits(integerFractionPair: List<String>)
            = integerFractionPair[0].length <= INTEGER_SIZE
            || (hasFraction(integerFractionPair) && integerFractionPair[1].length <= FRACTION_SIZE)

    private fun hasFraction(integerFractionPair: List<String>) = integerFractionPair.size == 2

    private fun copyStringToDigits(string: String, offset: Int): ByteArray {
        val integer = ByteArray(INTEGER_SIZE)
        for (i in string.indices) {
            integer[offset + i] = Character.getNumericValue(string[i]).toByte()
        }
        return integer
    }

    private fun BigDecimalSurrogate.toOriginal(): BigDecimal {
        val integer = this.integer.joinToString(separator = "") { "$it" }.trim('0')
        val fraction = this.fraction.joinToString(separator = "") { "$it" }.trim('0')
        var digit = if (fraction.isEmpty()) integer else "$integer.$fraction"
        if (this.sign == (-1).toByte()) {
            digit = "-$digit"
        }
        return BigDecimal(digit)
    }
}

@ExperimentalSerializationApi
@Suppress("ArrayInDataClass")
@Serializable
data class BigDecimalSurrogate(val sign: Byte, @FixedLength([100]) val integer: ByteArray, @FixedLength([20]) val fraction: ByteArray)