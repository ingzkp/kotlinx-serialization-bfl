package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")

class CompoundNullableListTest {
    @Serializable
    data class NullableData(@FixedLength([2]) val nested: Pair<Int, List<Int>?>)

    @Test
    fun `nullable List within a compound type should be serialized successfully`() {
        val mask = listOf(
            Pair("pair.first", 4),
            Pair("pair.second.isNull", 1),
            Pair("pair.second.length", 4),
            Pair("pair.second.value", 8),
        )

        listOf(
            NullableData(Pair(10, listOf(20))),
            NullableData(Pair(10, null)),
        ).forEach {
            checkedSerializeInlined(it, mask)
            checkedSerialize(it, mask)
        }
    }

    @Test
    fun `nullable List within a compound type should be the same after serialization and deserialization`() {
        val data = NullableData(Pair(10, null))

        roundTripInlined(data)
        roundTrip(data)
    }

    @Test
    fun `different nullable Lists within a compound type should have same size after serialization`() {
        val empty = NullableData(Pair(0, listOf()))
        val pair1 = NullableData(Pair(1, listOf(1)))
        val pair2 = NullableData(Pair(2, listOf(1, 2)))
        val pair3 = NullableData(Pair(2, null))

        sameSizeInlined(pair1, pair2)
        sameSize(pair1, pair2)
        sameSizeInlined(pair2, empty)
        sameSize(pair2, empty)
        sameSizeInlined(pair1, pair3)
        sameSize(pair1, pair3)
    }
}
