package com.ing.serialization.bfl.serde.serializers.custom

import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.util.UUID

class UUIDSerializerTest {
    @Serializable
    data class Data(val id: @Contextual UUID)

    @Test
    fun `UUID should be serialized successfully`() {
        val mask = listOf(
            Pair("mostSigBits.value", 8),
            Pair("leastSigBits.value", 8)
        )

        val data = Data(UUID.randomUUID())

        checkedSerializeInlined(data, mask)
        checkedSerialize(data, mask)
    }

    @Test
    fun `UUID should be the same after serialization and deserialization`() {
        val data = Data(UUID.randomUUID())
        roundTripInlined(data)
        roundTrip(data)
    }

    @Test
    fun `different UUIDs should have same size after serialization`() {
        val data1 = Data(UUID(0, 1))
        val data2 = Data(UUID(0, 2))

        sameSizeInlined(data1, data2)
        sameSize(data1, data2)
    }
}
