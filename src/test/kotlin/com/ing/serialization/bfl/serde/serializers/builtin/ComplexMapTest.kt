package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.deserialize
import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serialize
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")

class ComplexMapTest {
    @Serializable
    data class Data(@FixedLength([2, 2, 2]) val map: Map<String, List<Int>>)

    @Test
    fun `serialize complex map`() {
        val mask = listOf(
            Pair("map.length", 4),
            Pair("map[0].key", 2 + 2 * 2),
            Pair("map[0].value", 4 + 2 * 4),
            Pair("map[1].key", 2 + 2 * 2),
            Pair("map[1].value", 4 + 2 * 4),
        )

        var data = Data(mapOf("a" to listOf(2)))
        var bytes = checkedSerialize(data, mask)

        data = Data(mapOf())
        bytes = checkedSerialize(data, mask)
        bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
    }

    @Test
    fun `serialize and deserialize complex map`() {
        val data = Data(mapOf("a" to listOf(2)))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)
        data shouldBe deserialized
    }

    @Test
    fun `serialization has fixed length`() {
        val empty = Data(mapOf())
        val map1 = Data(mapOf("a" to listOf(1)))
        val map2 = Data(mapOf("a" to listOf(1), "b" to listOf(2, 3)))

        serialize(map1).size shouldBe serialize(map2).size
        serialize(map2).size shouldBe serialize(empty).size
    }
}
