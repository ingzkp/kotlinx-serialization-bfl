package com.ing.serialization.bfl.serde.exceptions

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serde.SerdeError
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.ing.serialization.bfl.api.serialize as serializeInlined

class CollectionTooLargeTest {
    @Serializable
    data class Data(@FixedLength([2, 2, 2, 2]) val myMap: Map<String, List<Int>>)

    @Test
    fun `String too large fails with StringTooLarge`() {
        val data = Data(mapOf("aaaa" to listOf(2)))

        assertThrows<SerdeError.StringTooLarge> {
            serializeInlined(data)
        }.also {
            println(it.message)
        }
    }

    @Test
    fun `List too large fails with CollectionTooLarge`() {
        val data = Data(mapOf("aa" to listOf(1, 2, 3)))

        assertThrows<SerdeError.CollectionTooLarge> {
            serializeInlined(data)
        }.also {
            println(it.message)
        }
    }
}
