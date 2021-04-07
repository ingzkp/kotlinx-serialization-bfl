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
class StringNullableTest {
    @Serializable
    data class NullableData(@FixedLength([10]) val s: String? = "123456789")

    @Test
    fun `nullable String should be serialized successfully`() {
        val mask = listOf(
            Pair("string.nonNull", 1),
            Pair("string.length", 2),
            Pair("string.value", 2 * 10)
        )

        var data = NullableData()
        listOf(
            checkedSerializeInlined(data, mask),
            checkedSerialize(data, mask),
        ).forEach { bytes ->
            assert(bytes[0] != 0.toByte()) { "A non-null value is expected" }
            bytes[2].toInt() shouldBe data.s?.length
        }

        data = NullableData(null)
        listOf(
            checkedSerializeInlined(data, mask),
            checkedSerialize(data, mask),
        ).forEach { bytes ->
            assert(bytes[0] == 0.toByte()) { "A null value is expected" }
        }
    }

    @Test
    fun `nullable String should be the same after serialization and deserialization`() {
        val data = NullableData(null)

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `different nullable Strings should have same size after serialization`() {
        val data1 = NullableData("1")
        val data2 = NullableData(null)

        sameSizeInlined(data1, data2)
        sameSize(data1, data2)
    }

    @Test
    fun `too long nullable String should throw StringTooLarge`() {
        assertThrows<SerdeError.StringTooLarge> {
            serialize(NullableData("12345678910"))
        }
    }
}
