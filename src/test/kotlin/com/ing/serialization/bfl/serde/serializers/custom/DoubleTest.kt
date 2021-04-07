package com.ing.serialization.bfl.serde.serializers.custom

import com.ing.serialization.bfl.api.reified.deserialize
import com.ing.serialization.bfl.api.reified.serialize
import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import com.ing.serialization.bfl.serializers.BFLSerializers
import com.ing.serialization.bfl.serializers.DoubleSurrogate
import io.kotest.matchers.doubles.shouldBeExactly
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

class DoubleTest {
    @Serializable
    data class Data(val value: Double?)

    @Test
    fun `Double should be serialized successfully`() {
        val mask = listOf(
            Pair("nonNull", 1),
            Pair("sign", 1),
            Pair("integer", 4 + DoubleSurrogate.DOUBLE_INTEGER_SIZE),
            Pair("fraction", 4 + DoubleSurrogate.DOUBLE_FRACTION_SIZE)
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
        roundTrip(data)
    }

    @Test
    fun `different Doubles should have same size after serialization`() {
        val double = (
            List(DoubleSurrogate.DOUBLE_INTEGER_SIZE / 10) { "1234567890" }.joinToString(separator = "") + "." +
                List(DoubleSurrogate.DOUBLE_FRACTION_SIZE / 10) { "1234567890" }.joinToString(separator = "")
            ).toDouble()

        val data1 = Data(4.33)
        val data2 = Data(double)
        val data3 = Data(null)

        sameSizeInlined(data1, data2)
        sameSize(data1, data3)
    }

    @Test
    fun `Serialization is lossless for min and max values`() {
        listOf(
            4.33,
            Double.MAX_VALUE,
            Double.MIN_VALUE,
        ).forEach { expected ->
            val serialized = serialize(Data(expected), BFLSerializers)
            val actual = deserialize<Data>(serialized)
            actual.value!! shouldBeExactly expected
        }
    }
}
