package com.ing.serialization.bfl.serde.classes

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.deserialize
import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serialize
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
@ExperimentalSerializationApi
class CompoundComplexMapTest {
    @Serializable
    data class Data(
        @FixedLength([2, 2, 2, 2])
        val nested: Triple<String, Int, Map<String, List<Int>>>
    )

    @Test
    fun `serialize complex map within a compound type`() {
        val mask = listOf(
            Pair("nested.1", 2 + 2 * 2),
            Pair("nested.2", 4),
            Pair("nested.3.length", 4),
            Pair("nested.3.map[0].key", 2 + 2 * 2),
            Pair("nested.3.map[0].value", 4 + 2 * 4),
            Pair("nested.3.map[1].key", 2 + 2 * 2),
            Pair("nested.3.map[1].value", 4 + 2 * 4),
        )

        var data = Data(Triple("a", 1, mapOf("a" to listOf(2))))
        checkedSerialize(data, mask)

        data = Data(Triple("a", 1, mapOf()))
        checkedSerialize(data, mask)
    }

    @Test
    fun `serialize and deserialize complex map within a compound type`() {
        val data = Data(Triple("a", 1, mapOf("a" to listOf(2))))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)
        data shouldBe deserialized
    }

    @Test
    fun `serialization has fixed length`() {
        val empty = Data(Triple("", 0, mapOf()))
        val data1 = Data(Triple("a", 1, mapOf("a" to listOf(2))))
        val data2 = Data(Triple("ba", 2, mapOf("ah" to listOf(2, 4))))

        serialize(data1).size shouldBe serialize(data2).size
        serialize(data2).size shouldBe serialize(empty).size
    }
}
