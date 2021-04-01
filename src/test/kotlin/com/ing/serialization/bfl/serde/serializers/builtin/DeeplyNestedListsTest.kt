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

class DeeplyNestedListsTest {
    @Serializable
    data class Data(
        @FixedLength([3, 4, 5])
        val nested: List<List<List<Int>>>
    )

    @Test
    fun `serialize deeply nested lists`() {
        val mask = listOf(
            Pair("nested.length", 4),
            Pair("nested[0].length", 4),
            Pair("nested[0][0].length", 4),
            Pair("nested[0][0].value", 4 * 5),
            Pair("nested[0][1].length", 4),
            Pair("nested[0][1].value", 4 * 5),
            Pair("nested[0][2].length", 4),
            Pair("nested[0][2].value", 4 * 5),
            Pair("nested[0][3].length", 4),
            Pair("nested[0][3].value", 4 * 5),
            Pair("nested[1].length", 4),
            Pair("nested[1][0].length", 4),
            Pair("nested[1][0].value", 4 * 5),
            Pair("nested[1][1].length", 4),
            Pair("nested[1][1].value", 4 * 5),
            Pair("nested[1][2].length", 4),
            Pair("nested[1][2].value", 4 * 5),
            Pair("nested[1][3].length", 4),
            Pair("nested[1][3].value", 4 * 5),
            Pair("nested[2].length", 4),
            Pair("nested[2][0].length", 4),
            Pair("nested[2][0].value", 4 * 5),
            Pair("nested[2][1].length", 4),
            Pair("nested[2][1].value", 4 * 5),
            Pair("nested[2][2].length", 4),
            Pair("nested[2][2].value", 4 * 5),
            Pair("nested[2][3].length", 4),
            Pair("nested[2][3].value", 4 * 5)
        )

        var data = Data(listOf(listOf(listOf(2))))
        var bytes = checkedSerializeInlined(data, mask)
        bytes.filter { it.toInt() != 0 }.sorted().distinct().toByteArray() shouldBe ByteArray(2) { (it + 1).toByte() }

        data = Data(listOf())
        bytes = checkedSerializeInlined(data, mask)
        bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
    }

    @Test
    fun `serialize and deserialize deeply nested lists`() {
        val data = Data(listOf(listOf(listOf(2))))

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `serialization has fixed length`() {
        val empty = Data(listOf(listOf(listOf<Int>())))
        val data1 = Data(listOf(listOf(listOf(2))))
        val data2 = Data(listOf(listOf(listOf(2)), listOf(listOf(4))))

        sameSizeInlined(empty, data1)
        sameSize(empty, data1)
        sameSizeInlined(data2, data1)
        sameSize(data2, data1)
    }
}
