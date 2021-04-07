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

class ListCompoundTest {
    @Serializable
    data class Data(@FixedLength([2]) val list: List<Pair<Int, Int>>)

    @Test
    fun `List of compound type should be serialized successfully`() {
        val mask = listOf(
            Pair("list.length", 4),
            Pair("list[0]", 8),
            Pair("list[1]", 8),
        )

        var data = Data(listOf(Pair(10, 20)))
        listOf(
            checkedSerializeInlined(data, mask),
            checkedSerialize(data, mask),
        ).forEach { bytes ->
            bytes[3].toInt() shouldBe 1
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
    fun `List of compound type should be the same after serialization and deserialization`() {
        val data = Data(listOf(Pair(10, 20)))

        roundTripInlined(data)
        roundTrip(data)
    }

    @Test
    fun `different Lists of compound type should have same size after serialization`() {
        val empty = Data(listOf())
        val data1 = Data(listOf(Pair(1, 2)))
        val data2 = Data(listOf(Pair(1, 2), Pair(4, 5)))

        sameSizeInlined(empty, data1)
        sameSize(empty, data1)
        sameSizeInlined(data2, data1)
        sameSize(data2, data1)
    }

    @Test
    fun `too long List of compound type should throw CollectionTooLarge`() {
        assertThrows<SerdeError.CollectionTooLarge> {
            serialize(Data(listOf(Pair(1, 2), Pair(3, 4), Pair(5, 6))))
        }
    }
}
