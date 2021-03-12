package serde.classes

import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import serde.ElementFactory
import serde.SerdeTest
import java.security.PublicKey

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
@ExperimentalSerializationApi
class NestedClassesPolymorphicTest: SerdeTest() {
    @Serializable
    data class Some(val pk: PublicKey)

    @Serializable
    data class Data(val some: Some)

    @Test
    fun `serialize polymorphic type within nested compound type`() {
        val mask = listOf(
            Pair("some.pk.serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("some.pk.length", 4),
            Pair("some.nested.value", 500)
        )

        val data = Data(Some(getRSA()))
        checkedSerialize(data, mask)
    }

    @Test
    fun `serialize and deserialize polymorphic type within nested compound type`() {
        val data = Data(Some(getRSA()))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)
        data shouldBe deserialized
    }

    @Test
    fun `serialization has fixed length`() {
        val data1 = Data(Some(getRSA()))
        val data2 = Data(Some(getRSA()))

        serialize(data1).size shouldBe serialize(data2).size
    }
}