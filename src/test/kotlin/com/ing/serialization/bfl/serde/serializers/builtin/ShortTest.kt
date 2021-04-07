package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

class ShortTest {
    @Serializable
    data class Data(val value: Short)

    @Test
    fun `Short should be serialized successfully`() {
        val mask = listOf(
            Pair("value", 2)
        )

        val data = Data(1)
        checkedSerializeInlined(data, mask)
        checkedSerialize(data, mask)
    }

    @Test
    fun `Short should be the same after serialization and deserialization`() {
        val data = Data(1)

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `different Shorts should have same size after serialization`() {
        val data1 = Data(1)
        val data2 = Data(2)

        sameSize(data2, data1)
        sameSizeInlined(data2, data1)
    }
}
