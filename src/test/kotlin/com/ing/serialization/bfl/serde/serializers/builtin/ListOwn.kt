package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serde.Own
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
class ListOwn {
    @Serializable
    data class Data(@FixedLength([2]) val list: List<Own>)

    @Test
    fun `serialize list with own serializable class`() {
        val mask = listOf(
            Pair("list.length", 4),
            Pair("list[0].value", 4),
            Pair("list[1].value", 4),
        )

        var data = Data(listOf(Own()))
        var bytes = checkedSerializeInlined(data, mask)

        data = Data(listOf())
        bytes = checkedSerializeInlined(data, mask)
        bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
    }

    @Test
    fun `serialize and deserialize list with own serializable class`() {
        val data = Data(listOf(Own()))

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `serialization has fixed length`() {
        val empty = Data(listOf())
        val data1 = Data(listOf(Own(1)))
        val data2 = Data(listOf(Own(1), Own(2)))

        sameSizeInlined(empty, data1)
        sameSize(empty, data1)
        sameSizeInlined(data2, data1)
        sameSize(data2, data1)
    }
}
