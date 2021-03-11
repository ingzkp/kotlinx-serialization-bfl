package serde

import annotations.DFLength
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
@ExperimentalSerializationApi
class MapTest: SerdeTest() {
    @Serializable
    data class Data(@DFLength([2]) val map: Map<Int, Int>)

    @Test
    fun `serialize plain map`() {
        val mask = listOf(
            Pair("map.length", 4),
            Pair("map[0].value", 8),
            Pair("map[1].value", 8),
        )

        var data = Data(mapOf(1 to 2))
        var bytes = checkedSerialize(data, mask)

        data = Data(mapOf())
        bytes = checkedSerialize(data, mask)
        bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
    }

    @Test
    fun `serialize and deserialize plain map`() {
        val data = Data(mapOf(1 to 2))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)
        data shouldBe deserialized
    }

    @Test
    fun `serialization has fixed length`() {
        serialize(Data(mapOf(1 to 2))).size shouldBe serialize(Data(mapOf(1 to 2, 2 to 3))).size
        serialize(Data(mapOf(1 to 2))).size shouldBe serialize(Data(mapOf())).size
    }
}