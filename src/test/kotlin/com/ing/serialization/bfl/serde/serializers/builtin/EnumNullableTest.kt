package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

class EnumNullableTest {
    enum class Status { SUPPORTED, UNSUPPORTED }

    @Serializable
    data class NullableData(val status: Status?)

    @Test
    fun `nullable Enum should be serialized successfully`() {
        val mask = listOf(
            Pair("nonNull", 1),
            Pair("value", 4)
        )

        var data = NullableData(Status.SUPPORTED)
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
    fun `nullable Enum should be the same after serialization and deserialization`() {
        val data = NullableData(null)

        roundTripInlined(data)
        roundTrip(data)
    }

    @Test
    fun `different nullable Enums should have same size after serialization`() {
        val data1 = NullableData(Status.SUPPORTED)
        val data2 = NullableData(Status.UNSUPPORTED)
        val data3 = NullableData(null)

        sameSize(data2, data1)
        sameSizeInlined(data2, data1)
        sameSize(data3, data1)
        sameSizeInlined(data3, data1)
    }
}
