package com.ing.serialization.bfl.serde.serializers.custom

import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSizeInlined
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import com.ing.serialization.bfl.api.reified.debugSerialize as debugSerializeInlined

class ZonedDateTimeSerializerTest {
    @Serializable
    data class Data(val date: @Contextual ZonedDateTime)

    @Test
    fun `serialize ZonedDateTime`() {
        val data = Data(ZonedDateTime.now())
        println(debugSerializeInlined(data).second)
    }

    @Test
    fun `serialize and deserialize ZonedDateTime`() {
        val data = Data(ZonedDateTime.now())
        roundTripInlined(data)
    }

    @Test
    fun `same size ZonedDateTime`() {
        val data1 = Data(ZonedDateTime.now())
        val data2 = Data(ZonedDateTime.now().minusDays(2))

        sameSizeInlined(data1, data2)
    }
}
