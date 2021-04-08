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
import com.ing.serialization.bfl.serializers.FloatSurrogate
import io.kotest.matchers.floats.shouldBeExactly
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

class FloatTest {
    @Serializable
    data class Data(val value: Float?)

    @Test
    fun `Float should be serialized successfully`() {
        val mask = listOf(
            Pair("nonNull", 1),
            Pair("sign", 1),
            Pair("integer", 4 + FloatSurrogate.FLOAT_INTEGER_SIZE),
            Pair("fraction", 4 + FloatSurrogate.FLOAT_FRACTION_SIZE)
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
    fun `Serialization is lossless for min and max values`() {
        listOf(
            4.33F,
            Float.MAX_VALUE,
            Float.MIN_VALUE,
        ).forEach { expected ->
            val serialized = serialize(Data(expected), serializersModule = BFLSerializers)
            val actual = deserialize<Data>(serialized)
            actual.value!! shouldBeExactly expected
        }
    }
}
