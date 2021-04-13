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
import java.time.Duration

class DurationSerializerTest {
    @Serializable
    data class Data(val duration: @Contextual Duration)

    @Test
    fun `Duration should be serialized successfully`() {
        val mask = listOf(
            Pair("seconds.value", 8),
            Pair("nanos.value", 4)
        )

        listOf(
            Duration.ofSeconds(1000, 1000),
            Duration.ofSeconds(-1000, 1000),
        ).forEach {
            checkedSerializeInlined(it, mask)
            checkedSerialize(it, mask)
        }
    }

    @Test
    fun `Duration should be the same after serialization and deserialization`() {
        val data = Data(Duration.ofSeconds(1000, 1000))
        roundTripInlined(data)
        roundTrip(data)
    }

    @Test
    fun `different Durations should have same size after serialization`() {
        val data1 = Data(Duration.ofSeconds(1000, 1000))
        val data2 = Data(Duration.ofSeconds(2000, 1000))

        sameSizeInlined(data1, data2)
        sameSize(data1, data2)
    }
}
