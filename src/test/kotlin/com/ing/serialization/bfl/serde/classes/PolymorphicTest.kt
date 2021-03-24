package com.ing.serialization.bfl.serde.classes

import com.ing.serialization.bfl.serde.SerdeTest
import com.ing.serialization.bfl.serde.element.ElementFactory
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.jupiter.api.Test
import java.security.PublicKey

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
@ExperimentalSerializationApi
class PolymorphicTest : SerdeTest() {
    @Test
    fun `serialize polymorphic type itself`() {
        val mask = listOf(
            Pair("serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("length", 4),
            Pair("value", 500)
        )

        val data = generatePublicKey()
        checkedSerialize(data, mask)
    }

    @Test
    fun `serialize and deserialize polymorphic type itself`() {
        val data = generatePublicKey()
        val bytes = serialize(data)

        val deserialized: PublicKey = deserialize(bytes)
        data shouldBe deserialized
    }
}
