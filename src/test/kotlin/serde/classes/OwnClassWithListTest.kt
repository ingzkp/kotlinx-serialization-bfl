package serde.classes

import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import serde.OwnList
import serde.SerdeTest

@ExperimentalSerializationApi
@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
class OwnClassWithListTest : SerdeTest() {
    @Serializable
    data class Data(val own: OwnList)

    @Test
    fun `serialize list with own (with list) serializable class`() {
        val mask = listOf(
            Pair("own.list.length", 4),
            Pair("own.list[0].value", 4),
            Pair("own.list[1].value", 4),
        )

        var data = Data(OwnList(listOf(10)))
        var bytes = checkedSerialize(data, mask)
        //
        data = Data(OwnList(listOf()))
        bytes = checkedSerialize(data, mask)
        bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
    }

    @Test
    fun `serialize and deserialize list with own (with list) serializable class`() {
        val data = Data(OwnList(listOf(10)))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)
        data shouldBe deserialized
    }

    @Test
    fun `serialization has fixed length`() {
        val empty = OwnList(listOf())
        val own1 = OwnList(listOf(10))
        val own2 = OwnList(listOf(10, 2))
        serialize(Data(own1)).size shouldBe serialize(Data(own2)).size
        serialize(Data(own2)).size shouldBe serialize(Data(empty)).size
    }
}
