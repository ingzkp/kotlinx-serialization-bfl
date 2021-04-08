package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

class BooleanTest {
    @Serializable
    data class Data(val value: Boolean)

    @Test
    fun `Boolean should be serialized successfully`() {
        val mask = listOf(
            Pair("value", 1)
        )

        val data = Data(true)
        checkedSerializeInlined(data, mask)
        checkedSerialize(data, mask)
    }

    @Test
    fun `Boolean should be the same after serialization and deserialization`() {
        val data = Data(true)

        roundTripInlined(data)
        roundTrip(data)
    }

    @Test
    fun `different Booleans should have same size after serialization`() {
        val data1 = Data(true)
        val data2 = Data(false)

        sameSize(data2, data1)
        sameSizeInlined(data2, data1)
    }
}
