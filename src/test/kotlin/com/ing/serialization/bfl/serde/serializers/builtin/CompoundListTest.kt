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

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")

class CompoundListTest {
    @Serializable
    data class Data(@FixedLength([2]) val nested: Pair<Int, List<Int>>)

    @Test
    fun `serialize list within a compound type`() {
        val mask = listOf(
            Pair("pair.first", 4),
            Pair("pair.second.length", 4),
            Pair("pair.second.value", 8),
        )

        var data = Data(Pair(10, listOf(20)))
        var bytes = checkedSerializeInlined(data, mask)

        data = Data(Pair(10, listOf()))
        bytes = checkedSerializeInlined(data, mask)
        bytes shouldBe ByteArray(mask.sumBy { it.second }) {
            if (it == 3) {
                10
            } else {
                0
            }
        }
    }

    @Test
    fun `serialize and deserialize list within a compound type`() {
        val data = Data(Pair(10, listOf(20)))

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `serialization has fixed length`() {
        val empty = Data(Pair(0, listOf<Int>()))
        val data1 = Data(Pair(1, listOf(1)))
        val data2 = Data(Pair(2, listOf(1, 2)))

        sameSizeInlined(empty, data1)
        sameSize(empty, data1)
        sameSizeInlined(data2, data1)
        sameSize(data2, data1)
    }
}
