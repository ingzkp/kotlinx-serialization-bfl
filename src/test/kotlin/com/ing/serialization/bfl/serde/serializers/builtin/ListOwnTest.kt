package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.serialize
import com.ing.serialization.bfl.serde.Own
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
class ListOwnTest {
    @Serializable
    data class Data(@FixedLength([2]) val list: List<Own>)

    @Test
    fun `List with own serializable class should be serialized successfully`() {
        val mask = listOf(
            Pair("list.length", 4),
            Pair("list[0].value", 4),
            Pair("list[1].value", 4),
        )

        var data = Data(listOf(Own()))
        checkedSerializeInlined(data, mask)
        checkedSerialize(data, mask)

        data = Data(listOf())
        listOf(
            checkedSerializeInlined(data, mask),
            checkedSerialize(data, mask),
        ).forEach { bytes ->
            bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
        }
    }

    @Test
    fun `List with own serializable class should be the same after serialization and deserialization`() {
        val data = Data(listOf(Own()))

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `different Lists with own serializable class should have same size after serialization`() {
        val empty = Data(listOf())
        val data1 = Data(listOf(Own(1)))
        val data2 = Data(listOf(Own(1), Own(2)))

        sameSizeInlined(empty, data1)
        sameSize(empty, data1)
        sameSizeInlined(data2, data1)
        sameSize(data2, data1)
    }

    @Test
    fun `too long List with own serializable class should throw CollectionTooLarge`() {
        assertThrows<SerdeError.CollectionTooLarge> {
            serialize(Data(listOf(Own(1), Own(2), Own(3))))
        }
    }
}
