package com.ing.serialization.bfl.serde.serializers.custom.polymorphic

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.serialize
import com.ing.serialization.bfl.serde.generateDSAPubKey
import com.ing.serialization.bfl.serde.generateRSAPubKey
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalStateException
import java.security.PublicKey

class PublicKeyDifferentSerializersTest {
    @Serializable
    data class Data(@FixedLength([2]) val list: List<PublicKey>)

    @Test
    fun `different variants of a polymorphic type should not coexist in a collection`() {
        val sameKeys = listOf(generateRSAPubKey(), generateRSAPubKey())
        assertDoesNotThrow {
            serialize(ListPolymorphicTest.Data(sameKeys), serializersModule = PolySerializers)
        }

        val differentKeys = listOf(generateRSAPubKey(), generateDSAPubKey())
        assertThrows<IllegalStateException> {
            serialize(ListPolymorphicTest.Data(differentKeys), serializersModule = PolySerializers)
        }.also {
            it.message shouldBe "Different implementations of the same base type are not allowed"
        }
    }

    @Serializable
    data class InnerData(@FixedLength([10]) val name: String, val key: PublicKey)

    @Serializable
    data class NestedData(@FixedLength([2]) val nested: List<InnerData>)

    @Test
    fun `different variants of a nested polymorphic type should not coexist in a collection`() {
        val inner1 = InnerData("inner1", generateRSAPubKey())
        val inner2 = InnerData("inner2", generateRSAPubKey())
        val inner3 = InnerData("inner3", generateDSAPubKey())

        assertDoesNotThrow {
            serialize(NestedData(listOf(inner1, inner2)), serializersModule = PolySerializers)
        }

        assertThrows<IllegalStateException> {
            serialize(NestedData(listOf(inner1, inner3)), serializersModule = PolySerializers)
        }.also {
            it.message shouldBe "Different implementations of the same base type are not allowed"
        }
    }
}
