package com.ing.serialization.bfl.serde.serializers.custom

import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import com.ing.serialization.bfl.serializers.BigDecimalSurrogate
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BigDecimalSerializerTest {
    @Serializable
    data class Data(val value: @Contextual BigDecimal) {
        override fun toString(): String {
            return "Data(value=${value.toPlainString()})"
        }
    }

    @Test
    fun `serialize BigDecimal`() {
        val mask = listOf(
            Pair("sign", 1),
            Pair("integer", 4 + BigDecimalSurrogate.INTEGER_SIZE),
            Pair("fraction", 4 + BigDecimalSurrogate.FRACTION_SIZE)
        )

        val data = Data(4.33.toBigDecimal())
        checkedSerializeInlined(data, mask)
        checkedSerialize(data, mask)
    }

    @Test
    fun `serialize and deserialize BigDecimal`() {
        val data = Data(4.33.toBigDecimal())
        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `serialize and deserialize BigDecimal ten`() {
        val data = Data(BigDecimal.TEN)
        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `serialize and deserialize BigDecimal one tenth`() {
        val data = Data(BigDecimal("0.01"))
        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `same size BigDecimal`() {
        val data1 = Data(4.33.toBigDecimal())
        val data2 = Data(BigDecimalSurrogate.MAX)

        sameSizeInlined(data1, data2)
        sameSize(data1, data2)
    }
}
