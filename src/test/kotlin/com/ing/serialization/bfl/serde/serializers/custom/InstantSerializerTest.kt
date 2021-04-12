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
import java.time.Instant
import com.ing.serialization.bfl.api.reified.debugSerialize as debugSerializeInlined

class InstantSerializerTest {
    @Serializable
    data class Data(val instant: @Contextual Instant)

    @Test
    fun `Instant should be serialized successfully`() {
        val mask = listOf(
            Pair("seconds.value", 8),
            Pair("nanos.value", 4)
        )

        val data = Data(Instant.now())

        checkedSerializeInlined(data, mask)
        checkedSerialize(data, mask)

        println(debugSerializeInlined(data).second)
    }

    @Test
    fun `Instant should be the same after serialization and deserialization`() {
        val data = Data(Instant.now())
        roundTripInlined(data)
        roundTrip(data)
    }

    @Test
    fun `different Instants should have same size after serialization`() {
        val data1 = Data(Instant.now())
        val data2 = Data(Instant.now().minusSeconds(3600))

        sameSizeInlined(data1, data2)
        sameSize(data1, data2)
    }
}
