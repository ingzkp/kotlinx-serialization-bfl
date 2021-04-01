package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.element.ElementFactory
import com.ing.serialization.bfl.serde.generateRSAPubKey
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.security.PublicKey

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")

class ListPolymorphicTest {
    @Serializable
    data class Data(@FixedLength([2]) val nested: List<PublicKey>)

    @Test
    fun `serialize polymorphic type within collection`() {
        val mask = listOf(
            Pair("nested.length", 4),
            Pair("nested[0].serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("nested[0].length", 4),
            Pair("nested[0].value", 294),
            Pair("nested[0].serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("nested[0].length", 4),
            Pair("nested[1].value", 294)
        )

        val data = Data(listOf(generateRSAPubKey()))
        checkedSerializeInlined(data, mask)
        checkedSerialize(data, mask)
    }

    @Test
    fun `serialize and deserialize polymorphic type within collection`() {
        val data = Data(listOf(generateRSAPubKey()))

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `serialization has fixed length`() {
        val empty = Data(listOf())
        val data1 = Data(listOf(generateRSAPubKey()))
        val data2 = Data(listOf(generateRSAPubKey()))

        sameSizeInlined(empty, data1)
        sameSize(empty, data1)
        sameSizeInlined(data2, data1)
        sameSize(data2, data1)
    }
}
