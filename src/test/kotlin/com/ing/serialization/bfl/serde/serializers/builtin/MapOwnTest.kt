package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.deserialize
import com.ing.serialization.bfl.serde.Own
import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serialize
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
@ExperimentalSerializationApi
class MapOwnTest {
    @Serializable
    data class Data(@FixedLength([3, 2]) val map: Map<String, Own>)

    @Test
    fun `serialization of map containing own class`() {
        val mask = listOf(
            Pair("map.length", 4),
            Pair("map[0].key", 6),
            Pair("map[0].value", 4),
            Pair("map[1].key", 6),
            Pair("map[1].value", 4),
            Pair("map[2].key", 6),
            Pair("map[2].value", 4),
        )

        var data = Data(mapOf("a" to Own(1), "b" to Own(2)))
        checkedSerialize(data, mask)

        data = Data(mapOf())
        val bytes = checkedSerialize(data, mask)
        bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
    }

    @Test
    fun `serialize and deserialize of map containing own class`() {
        val data = Data(mapOf("a" to Own(1), "b" to Own(2)))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)
        data shouldBe deserialized
    }

    @Test
    fun `serialization has fixed length`() {
        val empty = Data(mapOf())
        val data1 = Data(mapOf("a" to Own(1), "b" to Own(2)))
        val data2 = Data(mapOf("a" to Own(1)))

        serialize(data1).size shouldBe serialize(data2).size
        serialize(empty).size shouldBe serialize(data2).size
    }
}
