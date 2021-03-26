package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.deserialize
import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.element.ElementFactory
import com.ing.serialization.bfl.serde.generateRSAPubKey
import com.ing.serialization.bfl.serialize
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.security.PublicKey

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
@ExperimentalSerializationApi
class ClassPolymorphicTest {
    @Serializable
    data class Data(val pk: PublicKey)

    @Test
    fun `serialize polymorphic type within structure`() {
        val mask = listOf(
            Pair("pk.serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("pk.value", 4 + 500)
        )

        val data = Data(generateRSAPubKey())
        checkedSerialize(data, mask)
    }

    @Test
    fun `serialize and deserialize polymorphic type within structure`() {
        @Serializable
        data class Data(val pk: PublicKey)

        val data = Data(generateRSAPubKey())
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)
        data shouldBe deserialized
    }

    @Test
    fun `serialization has fixed length`() {
        val data1 = Data(generateRSAPubKey())
        val data2 = Data(generateRSAPubKey())

        serialize(data1).size shouldBe serialize(data2).size
    }
}
