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

class ListStringTest {
    @Serializable
    data class Data(@FixedLength([2, 10]) val list: List<String> = listOf("123456789"))

    @Test
    fun `List of String should be serialized successfully`() {
        val mask = listOf(
            Pair("list.length", 4),
            Pair("string.length", 2),
            Pair("string.value", 2 * 10),
            Pair("string.length", 2),
            Pair("string.value", 2 * 10)
        )

        var data = Data()
        listOf(
            checkedSerializeInlined(data, mask),
            checkedSerialize(data, mask),
        ).forEach { bytes ->
            bytes[3].toInt() shouldBe data.list.size
        }

        data = Data(listOf())
        listOf(
            checkedSerializeInlined(data, mask),
            checkedSerialize(data, mask),
        ).forEach { bytes ->
            bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
        }
    }

    @Test
    fun `List of String should be the same after serialization and deserialization`() {
        val data = Data()

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `different Lists of String should have same size after serialization`() {
        val empty = Data(listOf())
        val data1 = Data(listOf("1"))
        val data2 = Data(listOf("12", "3"))

        sameSizeInlined(empty, data1)
        sameSize(empty, data1)
        sameSizeInlined(data2, data1)
        sameSize(data2, data1)
    }

    @Test
    fun `too long List of String should throw CollectionTooLarge`() {
        assertThrows<SerdeError.CollectionTooLarge> {
            serialize(Data(listOf("1", "2", "3")))
        }
    }

    @Test
    fun `List of String with too long string should throw StringTooLarge`() {
        assertThrows<SerdeError.StringTooLarge> {
            serialize(Data(listOf("1", "12345678910")))
        }
    }
}
