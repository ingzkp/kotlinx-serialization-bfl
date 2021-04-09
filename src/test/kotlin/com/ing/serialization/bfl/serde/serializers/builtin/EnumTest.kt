package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

class EnumTest {
    enum class Status { SUPPORTED, UNSUPPORTED }

    @Serializable
    data class Data(val status: Status)

    @Test
    fun `Enum should be serialized successfully`() {
        val mask = listOf(
            Pair("value", 4)
        )

        listOf(
            Data(Status.SUPPORTED),
            Data(Status.UNSUPPORTED),
        ).forEach {
            checkedSerializeInlined(it, mask)
            checkedSerialize(it, mask)
        }
    }

    @Test
    fun `Enum should be the same after serialization and deserialization`() {
        val data = Data(Status.SUPPORTED)

        roundTripInlined(data)
        roundTrip(data)
    }

    @Test
    fun `different Enums should have same size after serialization`() {
        val data1 = Data(Status.SUPPORTED)
        val data2 = Data(Status.UNSUPPORTED)

        sameSize(data1, data2)
        sameSizeInlined(data1, data2)
    }
}
