package com.ing.serialization.bfl.serde.serializers.custom

import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serializers.BigDecimalSurrogate
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@ExperimentalSerializationApi
class BigDecimalSerializerTest {
    @Serializable
    data class Data(val value: @Contextual BigDecimal)

    @Test
    fun `serialize BigDecimal`() {
        val mask = listOf(
            Pair("sign", 1),
            Pair("integer", 4 + BigDecimalSurrogate.INTEGER_SIZE),
            Pair("fraction", 4 + BigDecimalSurrogate.FRACTION_SIZE)
        )

        val data = Data(4.33.toBigDecimal())
        checkedSerialize(data, mask)
    }

    @Test
    fun `serialize and deserialize BigDecimal`() {
        val data = Data(4.33.toBigDecimal())
        roundTrip(data)
    }

    @Test
    fun `same size BigDecimal`() {
        val data1 = Data(4.33.toBigDecimal())
        val data2 = Data(BigDecimalSurrogate.MAX)

        sameSize(data1, data2)
    }
}
