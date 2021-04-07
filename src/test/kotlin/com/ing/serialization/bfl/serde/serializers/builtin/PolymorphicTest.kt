package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.element.ElementFactory
import com.ing.serialization.bfl.serde.generateRSAPubKey
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import org.junit.jupiter.api.Test

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")

class PolymorphicTest {
    @Test
    fun `Polymorphic type itself should be serialized successfully`() {
        val data = generateRSAPubKey()

        println(data.encoded.joinToString(separator = ","))

        var mask = listOf(
            Pair("serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("length", 4),
            Pair("value", 294)
        )
        checkedSerializeInlined(data, mask)

        // mask = listOf(
        //     Pair("length", 4),
        //     Pair("value", 294)
        // )
        // checkedSerialize(data, mask)
    }

    @Test
    fun `Polymorphic type should be the same after serialization and deserialization`() {
        val data = generateRSAPubKey()

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `different Polymorphic data objects should have same size after serialization`() {
        val data1 = generateRSAPubKey()
        val data2 = generateRSAPubKey()

        sameSizeInlined(data1, data2)
        sameSize(data1, data2)
    }
}
