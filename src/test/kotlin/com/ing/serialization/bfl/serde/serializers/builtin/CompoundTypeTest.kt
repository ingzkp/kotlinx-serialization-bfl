package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

class CompoundTypeTest {
    @Serializable
    data class Data(val pair: Pair<Int, Int>)

    @Test
    fun `serialize compound type`() {
        val mask = listOf(Pair("pair", 8))

        val data = Data(Pair(10, 20))
        checkedSerializeInlined(data, mask)
    }

    @Test
    fun `serialize and deserialize compound type`() {
        val data = Data(Pair(10, 20))

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `serialization has fixed length`() {
        val data1 = Data(Pair(1, 2))
        val data2 = Data(Pair(-3, -4))

        sameSizeInlined(data2, data1)
        sameSize(data2, data1)
    }
}
