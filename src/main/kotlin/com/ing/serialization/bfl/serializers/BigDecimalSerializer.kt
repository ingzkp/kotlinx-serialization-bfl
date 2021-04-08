package com.ing.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

typealias BigDecimalSizes = FixedLength

object BigDecimalSerializer : KSerializer<BigDecimal> by (SurrogateSerializer(BigDecimalSurrogate.serializer()) { BigDecimalSurrogate.from(it) })

@Suppress("ArrayInDataClass")
@Serializable
data class BigDecimalSurrogate(
    override val sign: Byte,
    override val integer: ByteArray,
    override val fraction: ByteArray
) : FloatingPointSurrogate<BigDecimal> {
    override fun toOriginal() = toBigDecimal()

    companion object {
        fun from(bigDecimal: BigDecimal): BigDecimalSurrogate {
            val (sign, integer, fraction) = bigDecimal.asByteTriple()
            return BigDecimalSurrogate(sign, integer, fraction)
        }
    }
}
