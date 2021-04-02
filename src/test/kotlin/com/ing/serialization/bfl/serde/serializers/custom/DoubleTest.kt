package com.ing.serialization.bfl.serde.serializers.custom

import com.ing.serialization.bfl.api.serialize
import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import com.ing.serialization.bfl.serializers.BFLSerializers
import com.ing.serialization.bfl.serializers.BigDecimalSurrogate
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DoubleTest {
    @Serializable
    data class Data(val value: Double?)

    @Test
    fun `Double should be serialized successfully`() {
        val mask = listOf(
            Pair("nonNull", 1),
            Pair("sign", 1),
            Pair("integer", 4 + BigDecimalSurrogate.DOUBLE_INTEGER_SIZE),
            Pair("fraction", 4 + BigDecimalSurrogate.DOUBLE_FRACTION_SIZE)
        )

        val data = Data(4.33)
        checkedSerializeInlined(data, mask)
        checkedSerialize(data, mask)
    }

    @Test
    fun `Double should be the same after serialization and deserialization`() {
        var data = Data(4.33)
        roundTripInlined(data)

        data = Data(null)
        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `different Doubles should have same size after serialization`() {
        val data1 = Data(4.33)
        val double = (
            List(BigDecimalSurrogate.DOUBLE_INTEGER_SIZE / 10) { "1234567890" }.joinToString(separator = "") + "." +
                List(BigDecimalSurrogate.DOUBLE_FRACTION_SIZE / 10) { "1234567890" }.joinToString(separator = "")
            ).toDouble()

        val data2 = Data(double)
        val data3 = Data(null)

        sameSizeInlined(data1, data2)
        sameSize(data1, data3)
    }

    @Test
    fun `serialize Double should throw IllegalArgumentException when size limit is not respected`() {
        listOf(
            Double.MAX_VALUE,
            Double.MIN_VALUE,
        ).forEach {
            assertThrows<IllegalArgumentException> {
                serialize(Data(it), BFLSerializers)
            }.also {
                it.message shouldBe "Double is too large for BigDecimalSurrogate"
            }
        }
    }
}
