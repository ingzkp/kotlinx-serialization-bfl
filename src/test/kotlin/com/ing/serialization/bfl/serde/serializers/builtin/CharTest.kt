package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

class CharTest {
    @Serializable
    data class Data(val value: Char)

    @Test
    fun `Char should be serialized successfully`() {
        val mask = listOf(
            Pair("value", 2)
        )

        val data = Data('a')
        checkedSerializeInlined(data, mask)
        checkedSerialize(data, mask)
    }

    @Test
    fun `Char should be the same after serialization and deserialization`() {
        val data = Data('a')

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `different Chars should have same size after serialization`() {
        val data1 = Data('a')
        val data2 = Data('b')

        sameSize(data2, data1)
        sameSizeInlined(data2, data1)
    }
}
