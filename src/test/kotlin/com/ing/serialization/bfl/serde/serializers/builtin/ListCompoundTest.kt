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

class ListCompoundTest {
    @Serializable
    data class Data(@FixedLength([2]) val list: List<Pair<Int, Int>>)

    @Test
    fun `serialize list of compound type`() {
        val mask = listOf(
            Pair("list.length", 4),
            Pair("list[0]", 8),
            Pair("list[1]", 8),
        )

        var data = Data(listOf(Pair(10, 20)))
        var bytes = checkedSerializeInlined(data, mask)
        bytes[3].toInt() shouldBe 1

        data = Data(listOf())
        bytes = checkedSerializeInlined(data, mask)
        bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
    }

    @Test
    fun `serialize and deserialize list of compound type`() {
        @Serializable
        data class Data(@FixedLength([2]) val list: List<Pair<Int, Int>>)

        val data = Data(listOf(Pair(10, 20)))

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `serialization has fixed length`() {
        val empty = Data(listOf())
        val data1 = Data(listOf(Pair(1, 2)))
        val data2 = Data(listOf(Pair(1, 2), Pair(4, 5)))

        sameSizeInlined(empty, data1)
        sameSize(empty, data1)
        sameSizeInlined(data2, data1)
        sameSize(data2, data1)
    }
}
