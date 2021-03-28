package com.ing.serialization.bfl.serde.serializers.custom

import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serializers.DateSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat
import java.util.Date
@ExperimentalSerializationApi
class DateSerializerTest {
    @Serializable
    data class Data(val date: @Serializable(with = DateSerializer::class) Date)

    @Test
    fun `serialize Date`() {
        val mask = listOf(Pair("date", 8))

        val data = Data(SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"))
        checkedSerialize(data, mask)
    }

    @Test
    fun `serialize and deserialize Date`() {
        val data = Data(SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"))
        roundTrip(data)
    }

    @Test
    fun `same size Date`() {
        val data1 = Data(SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"))
        val data2 = Data(SimpleDateFormat("yyyy-MM-ddX").parse("2018-01-12+00"))

        sameSize(data1, data2)
    }
}
