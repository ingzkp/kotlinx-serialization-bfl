package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serde.Own
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")

class MapNullableOwnTest {
    @Serializable
    data class Data(@FixedLength([3, 2]) val map: Map<String, Own?>)

    @Test
    fun `serialization of map containing own class`() {
        val mask = listOf(
            Pair("map.length", 4),
            Pair("map[0].key", 6),
            Pair("map[0].value", 5),
            Pair("map[1].key", 6),
            Pair("map[1].value", 5),
            Pair("map[2].key", 6),
            Pair("map[2].value", 5),
        )

        val data = Data(mapOf("a" to Own(1), "b" to null))
        checkedSerializeInlined(data, mask)
    }

    @Test
    fun `serialize and deserialize of map containing own class`() {
        val data = Data(mapOf("a" to Own(1), "b" to null))

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `serialization has fixed length`() {
        val empty = Data(mapOf())
        val data1 = Data(mapOf("a" to Own(1), "b" to null))
        val data2 = Data(mapOf("a" to Own(1)))

        sameSizeInlined(empty, data1)
        sameSize(empty, data1)
        sameSizeInlined(data2, data1)
        sameSize(data2, data1)
    }
}
