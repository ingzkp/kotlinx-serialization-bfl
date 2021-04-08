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

class DeeplyNestedListsTest {
    @Serializable
    data class Data(
        @FixedLength([3, 4, 5])
        val nested: List<List<List<Int>>>
    )

    @Test
    fun `deeply nested Lists should be serialized successfully`() {
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
        listOf(
            checkedSerializeInlined(data, mask),
            checkedSerialize(data, mask),
        ).forEach { bytes ->
            bytes.filter { it.toInt() != 0 }.sorted().distinct().toByteArray() shouldBe ByteArray(2) { (it + 1).toByte() }
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
    fun `deeply nested Lists should be the same after serialization and deserialization`() {
        val data = Data(listOf(listOf(listOf(2))))

        roundTripInlined(data)
        roundTrip(data)
    }

    @Test
    fun `different deeply nested Lists should have same size after serialization`() {
        val empty = Data(listOf(listOf(listOf())))
        val data1 = Data(listOf(listOf(listOf(2))))
        val data2 = Data(listOf(listOf(listOf(2)), listOf(listOf(4))))

        sameSizeInlined(empty, data1)
        sameSize(empty, data1)
        sameSizeInlined(data2, data1)
        sameSize(data2, data1)
    }

    @Test
    fun `too long deep nested Lists should throw CollectionTooLarge in any nesting level`() {
        listOf(
            Data(listOf(listOf(listOf(1, 2, 3, 4, 5, 6)))),
            Data(listOf((1..5).map { listOf(it) })),
            Data((1..4).map { listOf(listOf(it)) }),
        ).forEach {
            assertThrows<SerdeError.CollectionTooLarge> {
                serialize(it)
            }
        }
    }
}
