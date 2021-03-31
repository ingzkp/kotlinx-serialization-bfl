package com.ing.serialization.bfl.serde.serializers.custom

import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serializeX
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class ZonedDateTimeSerializerTest {
    @Serializable
    data class Data(val date: @Contextual ZonedDateTime)

    @Test
    fun `serialize ZonedDateTime`() {
        val data = Data(ZonedDateTime.now())
        println(serializeX(data).second)
    }

    @Test
    fun `serialize and deserialize ZonedDateTime`() {
        val data = Data(ZonedDateTime.now())
        roundTrip(data)
    }

    @Test
    fun `same size ZonedDateTime`() {
        val data1 = Data(ZonedDateTime.now())
        val data2 = Data(ZonedDateTime.now().minusDays(2))

        sameSize(data1, data2)
    }
}
