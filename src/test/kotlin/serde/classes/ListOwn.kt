package serde.classes

import annotations.FixedLength
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import serde.Own
import serde.SerdeTest

@ExperimentalSerializationApi
@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
class ListOwn : SerdeTest() {
    @Serializable
    data class Data(@FixedLength([2]) val list: List<Own>)

    @Test
    fun `serialize list with own serializable class`() {
        val mask = listOf(
            Pair("list.length", 4),
            Pair("list[0].value", 4),
            Pair("list[1].value", 4),
        )

        var data = Data(listOf(Own()))
        var bytes = checkedSerialize(data, mask)

        data = Data(listOf())
        bytes = checkedSerialize(data, mask)
        bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
    }

    @Test
    fun `serialize and deserialize list with own serializable class`() {
        val data = Data(listOf(Own()))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)
        data shouldBe deserialized
    }

    @Test
    fun `serialization has fixed length`() {
        val list1 = listOf(Own(1))
        val list2 = listOf(Own(1), Own(2))
        serialize(Data(list1)).size shouldBe serialize(Data(list2)).size
        serialize(Data(list1)).size shouldBe serialize(Data(listOf())).size
    }
}
