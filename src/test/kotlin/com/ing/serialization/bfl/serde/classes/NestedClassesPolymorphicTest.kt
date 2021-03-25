package com.ing.serialization.bfl.serde.classes

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
class NestedClassesPolymorphicTest {
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

        val data = Data(Some(generateRSAPubKey()))
        checkedSerialize(data, mask)
    }

    @Test
    fun `serialize and deserialize polymorphic type within nested compound type`() {
        val data = Data(Some(generateRSAPubKey()))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)
        data shouldBe deserialized
    }

    @Test
    fun `serialization has fixed length`() {
        val data1 = Data(Some(generateRSAPubKey()))
        val data2 = Data(Some(generateRSAPubKey()))

        serialize(data1).size shouldBe serialize(data2).size
    }
}
