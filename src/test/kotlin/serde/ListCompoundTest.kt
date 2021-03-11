package serde

import annotations.DFLength
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@ExperimentalSerializationApi
class ListCompoundTest: SerdeTest() {
    @Serializable
    data class Data(@DFLength([2]) val list: List<Pair<Int, Int>>)

    @Test
    fun `serialize list of compound type`() {
        val mask = listOf(
            Pair("list.length", 4),
            Pair("list[0]", 8),
            Pair("list[1]", 8),
        )

        var data = Data(listOf(Pair(10, 20)))
        var bytes = checkedSerialize(data, mask)
        bytes[3].toInt() shouldBe 1

        data = Data(listOf())
        bytes = checkedSerialize(data, mask)
        bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
    }

    @Test
    fun `serialize and deserialize list of compound type`() {
        @Serializable
        data class Data(@DFLength([2]) val list: List<Pair<Int, Int>>)

        val data = Data(listOf(Pair(10, 20)))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)
        data shouldBe deserialized
    }

    @Test
    fun `serialization has fixed length`() {
        val list1 = listOf(Pair(1, 2))
        val list2 = listOf(Pair(1, 2), Pair(4, 5))
        serialize(Data(list1)).size shouldBe serialize(Data(list2)).size
        serialize(Data(list2)).size shouldBe serialize(Data(listOf())).size
    }
}
