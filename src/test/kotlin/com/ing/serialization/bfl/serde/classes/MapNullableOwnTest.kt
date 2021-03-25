package com.ing.serialization.bfl.serde.classes

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
        checkedSerialize(data, mask)
    }

    @Test
    fun `serialize and deserialize of map containing own class`() {
        val data = Data(mapOf("a" to Own(1), "b" to null))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)
        data shouldBe deserialized
    }

    @Test
    fun `serialization has fixed length`() {
        val empty = Data(mapOf())
        val data1 = Data(mapOf("a" to Own(1), "b" to null))
        val data2 = Data(mapOf("a" to Own(1)))

        serialize(data1).size shouldBe serialize(data2).size
        serialize(empty).size shouldBe serialize(data2).size
    }
}
