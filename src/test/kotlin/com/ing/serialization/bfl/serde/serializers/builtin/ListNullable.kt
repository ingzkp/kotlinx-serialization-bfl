package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")

class ListNullable {
    @Serializable
    data class NullableData(@FixedLength([3]) val list: List<Int?>)

    @Test
    fun `serialize list with a primitive nullable type`() {
        val mask = listOf(
            Pair("list.length", 4),
            Pair("list[0]", 1 + 4),
            Pair("list[1]", 1 + 4),
            Pair("list[2]", 1 + 4),
        )

        val data = NullableData(listOf(25, null))
        checkedSerializeInlined(data, mask)
    }

    @Test
    fun `serialize and deserialize list with a primitive nullable type`() {
        val data = NullableData(listOf(25, null))

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `serialization has fixed length`() {
        val empty = NullableData(listOf())
        val data1 = NullableData(listOf(25))
        val data2 = NullableData(listOf(null, null))

        sameSizeInlined(empty, data1)
        sameSize(empty, data1)
        sameSizeInlined(data2, data1)
        sameSize(data2, data1)
    }
}
