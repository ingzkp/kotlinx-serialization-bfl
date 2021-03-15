package serde.classes

import annotations.FixedLength
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.jupiter.api.Test
import kotlinx.serialization.Serializable
import serde.SerdeTest

@ExperimentalSerializationApi
class DeeplyNestedListsTest: SerdeTest() {
    @Serializable
    data class Data(
        @FixedLength([  3,    4,   5])
        val nested: List<List<List<Int>>>)

    @Test
    fun `serialize deeply nested lists`() {
        val mask = listOf(
            Pair("nested.length", 4),
            Pair("nested[0].length", 4),
            Pair("nested[0][0].length", 4),
            Pair("nested[0][0].value", 4 * 5),
            Pair("nested[0][1].length", 4),
            Pair("nested[0][1].value", 4 * 5),
            Pair("nested[0][2].length", 4),
            Pair("nested[0][2].value", 4 * 5),
            Pair("nested[0][3].length", 4),
            Pair("nested[0][3].value", 4 * 5),
            Pair("nested[1].length", 4),
            Pair("nested[1][0].length", 4),
            Pair("nested[1][0].value", 4 * 5),
            Pair("nested[1][1].length", 4),
            Pair("nested[1][1].value", 4 * 5),
            Pair("nested[1][2].length", 4),
            Pair("nested[1][2].value", 4 * 5),
            Pair("nested[1][3].length", 4),
            Pair("nested[1][3].value", 4 * 5),
            Pair("nested[2].length", 4),
            Pair("nested[2][0].length", 4),
            Pair("nested[2][0].value", 4 * 5),
            Pair("nested[2][1].length", 4),
            Pair("nested[2][1].value", 4 * 5),
            Pair("nested[2][2].length", 4),
            Pair("nested[2][2].value", 4 * 5),
            Pair("nested[2][3].length", 4),
            Pair("nested[2][3].value", 4 * 5)
        )

        var data = Data(listOf(listOf(listOf(2))))
        var bytes = checkedSerialize(data, mask)
        bytes.filter { it.toInt() != 0 }.sorted().distinct().toByteArray() shouldBe ByteArray(2) { (it + 1).toByte() }

        data = Data(listOf())
        bytes = checkedSerialize(data, mask)
        bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
    }

    @Test
    fun `serialize and deserialize deeply nested lists`() {
        val data = Data(listOf(listOf(listOf(2))))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)
        data shouldBe deserialized
    }

    @Test
    fun `serialization has fixed length`() {
        val empty = listOf(listOf(listOf<Int>()))
        val data1 = listOf(listOf(listOf(2)))
        val data2 = listOf(listOf(listOf(2)), listOf(listOf(4)))

        serialize(Data(data1)).size shouldBe serialize(Data(data2)).size
        serialize(Data(data1)).size shouldBe serialize(Data(empty)).size
    }
}