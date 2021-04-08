package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.serde.OwnList
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
class OwnClassWithListTest {
    @Serializable
    data class Data(val own: OwnList)

    @Test
    fun `list with own (with list) serializable class should be serialized successfully`() {
        val mask = listOf(
            Pair("own.list.length", 4),
            Pair("own.list[0].value", 4),
            Pair("own.list[1].value", 4),
        )

        var data = Data(OwnList(listOf(10)))
        checkedSerializeInlined(data, mask)
        checkedSerialize(data, mask)

        data = Data(OwnList(listOf()))
        listOf(
            checkedSerializeInlined(data, mask),
            checkedSerialize(data, mask),
        ).forEach { bytes ->
            bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
        }
    }

    @Test
    fun `list with own (with list) serializable class should be the same after serialization and deserialization`() {
        val data = Data(OwnList(listOf(10)))

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `different lists with own (with list) serializable class should have same size after serialization`() {
        val empty = Data(OwnList(listOf()))
        val own1 = Data(OwnList(listOf(10)))
        val own2 = Data(OwnList(listOf(10, 2)))

        sameSize(empty, own1)
        sameSizeInlined(empty, own1)
        sameSize(own2, own1)
        sameSizeInlined(own2, own1)
    }
}
