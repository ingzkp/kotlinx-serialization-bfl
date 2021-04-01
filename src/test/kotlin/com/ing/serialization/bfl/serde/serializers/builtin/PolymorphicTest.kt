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
    fun `serialize polymorphic type itself`() {
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
    fun `serialize and deserialize polymorphic type itself`() {
        val data1 = generateRSAPubKey()
        val data2 = generateRSAPubKey()

        roundTripInlined(data1)
        roundTrip(data1, data1::class)

        sameSizeInlined(data1, data2)
        sameSize(data1, data2)
    }
}
