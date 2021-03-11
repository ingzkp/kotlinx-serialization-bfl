package serde

import annotations.DFLength
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
@ExperimentalSerializationApi
class CompoundListTest: SerdeTest() {
    @Serializable
    data class Data(@DFLength([2]) val nested: Pair<Int, List<Int>>)

    @Test
    fun `serialize list within a compound type`() {
        val mask = listOf(
            Pair("pair.first", 4),
            Pair("pair.second.length", 4),
            Pair("pair.second.value", 8),
        )

        var data = Data(Pair(10, listOf(20)))
        var bytes = checkedSerialize(data, mask)

        data = Data(Pair(10, listOf()))
        bytes = checkedSerialize(data, mask)
        bytes shouldBe ByteArray(mask.sumBy { it.second }) { if (it == 3) { 10 } else { 0 } }
    }

    @Test
    fun `serialize and deserialize list within a compound type`() {
        @Serializable
        data class Data(@DFLength([2]) val nested: Pair<Int, List<Int>>)

        val data = Data(Pair(10, listOf(20)))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)
        data shouldBe deserialized
    }

    @Test
    fun `serialization has fixed length`() {
        val empty = Pair(0, listOf<Int>())
        val pair1 = Pair(1, listOf(1))
        val pair2 = Pair(2, listOf(1, 2))
        serialize(Data(pair1)).size shouldBe serialize(Data(pair2)).size
        serialize(Data(pair2)).size shouldBe serialize(Data(empty)).size
    }
}