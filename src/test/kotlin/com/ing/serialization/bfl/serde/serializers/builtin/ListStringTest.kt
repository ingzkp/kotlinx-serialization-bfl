package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

class ListStringTest {
    @Serializable
    data class Data(@FixedLength([2, 10]) val list: List<String> = listOf("123456789"))

    @Test
    fun `serialize list of string`() {
        val mask = listOf(
            Pair("list.length", 4),
            Pair("string.length", 2),
            Pair("string.value", 2 * 10),
            Pair("string.length", 2),
            Pair("string.value", 2 * 10)
        )

        var data = Data()
        var bytes = checkedSerializeInlined(data, mask)
        bytes[3].toInt() shouldBe data.list.size

        data = Data(listOf())
        bytes = checkedSerializeInlined(data, mask)
        bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
    }

    @Test
    fun `serialize and deserialize list of string`() {
        val data = Data()

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `serialization has fixed length`() {
        val empty = Data(listOf())
        val data1 = Data(listOf("1"))
        val data2 = Data(listOf("12", "3"))

        sameSizeInlined(empty, data1)
        sameSize(empty, data1)
        sameSizeInlined(data2, data1)
        sameSize(data2, data1)
    }
}
