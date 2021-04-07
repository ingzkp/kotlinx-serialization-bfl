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

class FloatTest {
    @Serializable
    data class Data(val value: Float?)

    @Test
    fun `Float should be serialized successfully`() {
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
    fun `Float should be the same after serialization and deserialization`() {
        var data = Data(4.33.toFloat())
        roundTripInlined(data)

        data = Data(null)
        roundTripInlined(data)
        roundTrip(data)
    }

    @Test
    fun `different Floats should have same size after serialization`() {
        val data1 = Data(4.33.toFloat())
        val data2 = Data(Float.MAX_VALUE)
        val data3 = Data(null)

        sameSizeInlined(data1, data2)
        sameSize(data1, data3)
    }

    @Test
    fun `serialize Float should throw IllegalArgumentException when size limit is not respected`() {
        val floatOverSized = Float.MIN_VALUE

        assertThrows<IllegalArgumentException> {
            serialize(Data(floatOverSized), BFLSerializers)
        }.also {
            it.message shouldBe "Float is too large for BigDecimalSurrogate"
        }
    }
}
