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
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")

class ListNullableTest {
    @Serializable
    data class NullableData(@FixedLength([3]) val list: List<Int?>)

    @Test
    fun `List with a primitive nullable type should be serialized successfully`() {
        val mask = listOf(
            Pair("list.length", 4),
            Pair("list[0]", 1 + 4),
            Pair("list[1]", 1 + 4),
            Pair("list[2]", 1 + 4),
        )

        val data = NullableData(listOf(25, null))
        checkedSerializeInlined(data, mask)
        checkedSerialize(data, mask)
    }

    @Test
    fun `List with a primitive nullable type should be the same after serialization and deserialization`() {
        val data = NullableData(listOf(25, null))

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `different Lists with a primitive nullable type should have same size after serialization`() {
        val empty = NullableData(listOf())
        val data1 = NullableData(listOf(25))
        val data2 = NullableData(listOf(null, null))

        sameSizeInlined(empty, data1)
        sameSize(empty, data1)
        sameSizeInlined(data2, data1)
        sameSize(data2, data1)
    }

    @Test
    fun `too long List with a primitive nullable type should throw CollectionTooLarge`() {
        assertThrows<SerdeError.CollectionTooLarge> {
            serialize(NullableData(listOf(1, 2, 3, null)))
        }
    }
}
