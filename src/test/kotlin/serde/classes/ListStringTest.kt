package serde.classes

import annotations.FixedLength
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import serde.SerdeTest

@ExperimentalSerializationApi
class ListStringTest : SerdeTest() {
    @Serializable
    data class Data(@FixedLength([2, 10]) val list: List<String> = listOf("123456789"))

    @Test
    fun `serialize list of string`() {
        val mask = listOf(
            Pair("list.length", 4),
            Pair("string.length", 2),
            Pair("string.value", 2 * 10),
            Pair("string.length", 2),
            Pair("string.value", 2 * 10)
        )

        var data = Data()
        var bytes = checkedSerialize(data, mask)
        bytes[3].toInt() shouldBe data.list.size

        data = Data(listOf())
        bytes = checkedSerialize(data, mask)
        bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
    }

    @Test
    fun `serialize and deserialize list of string`() {
        val data = Data()
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)
        data shouldBe deserialized
    }

    @Test
    fun `serialization has fixed length`() {
        serialize(Data(listOf("1"))).size shouldBe serialize(Data(listOf("12", "3"))).size
        serialize(Data(listOf("1"))).size shouldBe serialize(Data(listOf())).size
    }
}
