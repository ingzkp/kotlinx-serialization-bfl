package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.serialize
import com.ing.serialization.bfl.serde.SerdeError
import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
class StringTest {
    @Serializable
    data class Data(@FixedLength([10]) val s: String = "123456789")

    @Test
    fun `String should be serialized successfully`() {
        val mask = listOf(
            Pair("string.length", 2),
            Pair("string.value", 2 * 10)
        )

        listOf(
            Data(),
            Data(""),
        ).forEach { data ->
            listOf(
                checkedSerializeInlined(data, mask),
                checkedSerialize(data, mask),
            ).forEach { bytes ->
                bytes[1].toInt() shouldBe data.s.length
            }
        }
    }

    @Test
    fun `String should be the same after serialization and deserialization`() {
        val data = Data()

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `different Strings should have same size after serialization`() {
        val data1 = Data("1")
        val data2 = Data("12")

        sameSizeInlined(data1, data2)
        sameSize(data1, data2)
    }

    @Test
    fun `too long String should throw StringTooLarge`() {
        assertThrows<SerdeError.StringTooLarge> {
            serialize(Data("12345678910"))
        }
    }
}
