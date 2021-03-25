package com.ing.serialization.bfl.serde.exceptions

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serde.SerdeError
import com.ing.serialization.bfl.serialize
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
@ExperimentalSerializationApi
class InsufficientLengthDataTest {
    @Serializable
    data class Data(@FixedLength([2, 2]) val map: Map<String, List<Int>>)

    @Test
    fun `direct property insufficient length`() {
        val data = Data(mapOf("a" to listOf(2)))

        val exception = assertThrows<SerdeError> {
            serialize(data)
        }

        exception.message shouldBe "Property ${Data::class.qualifiedName}.map cannot be parsed"
    }

    @Serializable
    data class ComplexData(@FixedLength([2]) val list: List<Data>)

    @Test
    fun `indirect property insufficient length`() {
        val data = ComplexData(listOf(Data(mapOf("a" to listOf(2)))))

        val exception = assertThrows<SerdeError> {
            serialize(data)
        }

        exception.message shouldBe "Property ${Data::class.qualifiedName}.map cannot be parsed"
    }
}
