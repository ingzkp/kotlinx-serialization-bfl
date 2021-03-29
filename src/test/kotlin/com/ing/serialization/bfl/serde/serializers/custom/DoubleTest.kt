package com.ing.serialization.bfl.serde.serializers.custom

import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serializers.BigDecimalSurrogate
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

@ExperimentalSerializationApi
class DoubleTest {
    @Serializable
    data class Data(val value: Double?)

    @Test
    fun `serialize Double`() {
        val mask = listOf(
            Pair("nonNull", 1),
            Pair("sign", 1),
            Pair("integer", 4 + BigDecimalSurrogate.INTEGER_SIZE),
            Pair("fraction", 4 + BigDecimalSurrogate.FRACTION_SIZE)
        )

        val data = Data(4.33)
        checkedSerialize(data, mask)
    }

    @Test
    fun `serialize and deserialize Double`() {
        var data = Data(4.33)
        roundTrip(data)

        data = Data(null)
        roundTrip(data)
    }

    @Test
    fun `same size Double`() {
        val data1 = Data(4.33)
        val double = (
            List(BigDecimalSurrogate.INTEGER_SIZE / 10) { "1234567890" }.joinToString(separator = "") + "." +
                List(BigDecimalSurrogate.FRACTION_SIZE / 10) { "1234567890" }.joinToString(separator = "")
            ).toDouble()

        val data2 = Data(double)
        val data3 = Data(null)

        sameSize(data1, data2)
        sameSize(data1, data3)
    }
}
