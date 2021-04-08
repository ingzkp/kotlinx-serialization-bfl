package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

class NullableIntTest {
    @Serializable
    data class NullableData(val int: Int?)

    @Test
    fun `nullable Int should be serialized successfully`() {
        val mask = listOf(
            Pair("nonNull", 1),
            Pair("value", 4)
        )

        var data = NullableData(2)
        listOf(
            checkedSerializeInlined(data, mask),
            checkedSerialize(data, mask),
        ).forEach { bytes ->
            assert(bytes[0] != 0.toByte()) { "A non-null value is expected" }
        }

        data = NullableData(null)
        listOf(
            checkedSerializeInlined(data, mask),
            checkedSerialize(data, mask),
        ).forEach { bytes ->
            assert(bytes[0] == 0.toByte()) { "A null value is expected" }
        }
    }

    @Test
    fun `nullable Int should be the same after serialization and deserialization`() {
        val data = NullableData(null)

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `different nullable Ints should have same size after serialization`() {
        val own1 = NullableData(null)
        val own2 = NullableData(1)

        sameSize(own2, own1)
        sameSizeInlined(own2, own1)
    }
}
