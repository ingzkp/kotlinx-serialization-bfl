package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serde.checkedSerialize
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
    fun `List within a compound type should be serialized successfully`() {
        val mask = listOf(
            Pair("pair.first", 4),
            Pair("pair.second.length", 4),
            Pair("pair.second.value", 8),
        )

        var data = Data(Pair(10, listOf(20)))
        checkedSerializeInlined(data, mask)
        checkedSerialize(data, mask)

        data = Data(Pair(10, listOf()))
        listOf(
            checkedSerializeInlined(data, mask),
            checkedSerialize(data, mask),
        ).forEach { bytes ->
            bytes shouldBe ByteArray(mask.sumBy { it.second }) {
                if (it == 3) {
                    10
                } else {
                    0
                }
            }
        }
    }

    @Test
    fun `List within a compound type should be the same after serialization and deserialization`() {
        val data = Data(Pair(10, listOf(20)))

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `different Lists within a compound type should have same size after serialization`() {
        val empty = Data(Pair(0, listOf<Int>()))
        val data1 = Data(Pair(1, listOf(1)))
        val data2 = Data(Pair(2, listOf(1, 2)))

        sameSizeInlined(empty, data1)
        sameSize(empty, data1)
        sameSizeInlined(data2, data1)
        sameSize(data2, data1)
    }
}
