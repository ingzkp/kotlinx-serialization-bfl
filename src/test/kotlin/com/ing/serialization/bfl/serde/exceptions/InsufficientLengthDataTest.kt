package com.ing.serialization.bfl.serde.exceptions

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serde.SerdeError
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.ing.serialization.bfl.api.serialize as serializeInlined

class InsufficientLengthDataTest {
    @Serializable
    data class Data(@FixedLength([2, 2]) val myMap: Map<String, List<Int>>)

    @Test
    fun `direct property insufficient length`() {
        val data = Data(mapOf("a" to listOf(2)))
        val exception = assertThrows<SerdeError> {
            serializeInlined(data)
        }

        exception.message shouldBe "Insufficient length data along the chain Data.myMap.ArrayList"
    }

    @Serializable
    data class ComplexData(@FixedLength([2]) val myList: List<Data>)

    @Test
    fun `Insufficient length deep along the hierarchy`() {
        val data = ComplexData(listOf(Data(mapOf("a" to listOf(2)))))
        val exception = assertThrows<SerdeError> {
            serializeInlined(data)
        }

        exception.message shouldBe "Insufficient length data along the chain ComplexData.myList.myMap.ArrayList"
    }

    @Serializable
    data class LocalData(val participants: List<Int>)
    @Serializable
    data class Wrapper(val localData: LocalData)

    @Test
    fun `Insufficient length shallow along the hierarchy`() {
        val data = Wrapper(LocalData(listOf(1)))
        val exception = assertThrows<SerdeError> {
            serializeInlined(data)
        }

        exception.message shouldBe "Insufficient length data along the chain Wrapper.localData.participants.ArrayList"
    }
}
