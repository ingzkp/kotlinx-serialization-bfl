package com.ing.serialization.bfl.serde.serializers.builtin

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
    fun `serialize nullable int`() {
        val mask = listOf(
            Pair("nonNull", 1),
            Pair("value", 4)
        )

        var data = NullableData(2)
        var bytes = checkedSerializeInlined(data, mask)
        assert(bytes[0] != 0.toByte()) { "A non-null value is expected" }

        data = NullableData(null)
        bytes = checkedSerializeInlined(data, mask)
        assert(bytes[0] == 0.toByte()) { "A null value is expected" }
    }

    @Test
    fun `serialize and deserialize nullable int`() {
        val data = NullableData(null)

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `serialization has fixed length`() {
        val own1 = NullableData(null)
        val own2 = NullableData(1)

        sameSize(own2, own1)
        sameSizeInlined(own2, own1)
    }
}
