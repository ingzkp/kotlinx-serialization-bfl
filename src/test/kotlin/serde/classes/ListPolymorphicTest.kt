package serde.classes

import annotations.DFLength
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import serde.ElementFactory
import serde.SerdeTest
import java.security.PublicKey

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
@ExperimentalSerializationApi
class ListPolymorphicTest: SerdeTest() {
    @Serializable
    data class Data(@DFLength([2]) val nested: List<PublicKey>)

    @Test
    fun `serialize polymorphic type within collection`() {
        val mask = listOf(
            Pair("nested.length", 4),
            Pair("nested[0].serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("nested[0].length", 4),
            Pair("nested[0].value", 500),
            Pair("nested[0].serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("nested[0].length", 4),
            Pair("nested[1].value", 500)
        )

        val data = Data(listOf(getRSA()))
        checkedSerialize(data, mask)
    }

    @Test
    fun `serialize and deserialize polymorphic type within collection`() {
        val data = Data(listOf(getRSA()))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)
        data shouldBe deserialized
    }

    @Test
    fun `serialization has fixed length`() {
        val empty = Data(listOf())
        val data1 = Data(listOf(getRSA()))
        val data2 = Data(listOf(getRSA()))

        serialize(data1).size shouldBe serialize(data2).size
        serialize(empty).size shouldBe serialize(data2).size
    }
}