package com.ing.serialization.bfl.serde.serializers.custom

import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import com.ing.serialization.bfl.serializers.BigDecimalSurrogate
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

class FloatTest {
    @Serializable
    data class Data(val value: Float?)

    @Test
    fun `serialize Float`() {
        val mask = listOf(
            Pair("nonNull", 1),
            Pair("sign", 1),
            Pair("integer", 4 + BigDecimalSurrogate.INTEGER_SIZE),
            Pair("fraction", 4 + BigDecimalSurrogate.FRACTION_SIZE)
        )

        val data = Data(4.33.toFloat())
        checkedSerializeInlined(data, mask)
        checkedSerialize(data, mask)
    }

    @Test
    fun `serialize and deserialize Float`() {
        var data = Data(4.33.toFloat())
        roundTripInlined(data)

        data = Data(null)
        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `same size Float`() {
        val data1 = Data(4.33.toFloat())
        val data2 = Data(Float.MAX_VALUE)
        val data3 = Data(null)

        sameSizeInlined(data1, data2)
        sameSize(data1, data3)
    }
}
