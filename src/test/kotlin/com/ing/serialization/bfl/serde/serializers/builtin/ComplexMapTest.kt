package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")

class ComplexMapTest {
    @Serializable
    data class Data(@FixedLength([2, 2, 2]) val map: Map<String, List<Int>>)

    @Test
    fun `complex Map should be serialized successfully`() {
        val mask = listOf(
            Pair("map.length", 4),
            Pair("map[0].key", 2 + 2 * 2),
            Pair("map[0].value", 4 + 2 * 4),
            Pair("map[1].key", 2 + 2 * 2),
            Pair("map[1].value", 4 + 2 * 4),
        )

        var data = Data(mapOf("a" to listOf(2)))
        checkedSerializeInlined(data, mask)
        checkedSerialize(data, mask)

        data = Data(mapOf())
        listOf(
            checkedSerializeInlined(data, mask),
            checkedSerialize(data, mask),
        ).forEach { bytes ->
            bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
        }
    }

    @Test
    fun `complex Map should be the same after serialization and deserialization`() {
        val data = Data(mapOf("a" to listOf(2)))

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `different complex Maps should have same size after serialization`() {
        val empty = Data(mapOf())
        val data1 = Data(mapOf("a" to listOf(1)))
        val data2 = Data(mapOf("a" to listOf(1), "b" to listOf(2, 3)))

        sameSizeInlined(empty, data1)
        sameSize(empty, data1)
        sameSizeInlined(data2, data1)
        sameSize(data2, data1)
    }
}
