package com.ing.serialization.bfl.serde.classes

import com.ing.serialization.bfl.serde.ElementFactory
import com.ing.serialization.bfl.serde.SerdeTest
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.security.PublicKey

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
@ExperimentalSerializationApi
class ClassPolymorphicTest : SerdeTest() {
    @Serializable
    data class Data(val pk: PublicKey)

    @Test
    fun `serialize polymorphic type within structure`() {
        val mask = listOf(
            Pair("pk.serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("pk.value", 4 + 500)
        )

        val data = Data(getRSA())
        checkedSerialize(data, mask)
    }

    @Test
    fun `serialize and deserialize polymorphic type within structure`() {
        @Serializable
        data class Data(val pk: PublicKey)

        val data = Data(getRSA())
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)
        data shouldBe deserialized
    }

    @Test
    fun `serialization has fixed length`() {
        val data1 = Data(getRSA())
        val data2 = Data(getRSA())

        serialize(data1).size shouldBe serialize(data2).size
    }
}
