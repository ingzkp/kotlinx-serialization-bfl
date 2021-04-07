package com.ing.serialization.bfl.serde.serializers.custom

import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import com.ing.serialization.bfl.serializers.DateSerializer
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat
import java.util.Date

class DateSerializerTest {
    @Serializable
    data class Data(val date: @Serializable(with = DateSerializer::class) Date)

    @Test
    fun `Date should be serialized successfully`() {
        val mask = listOf(Pair("date", 8))

        val data = Data(SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"))
        checkedSerializeInlined(data, mask)
        checkedSerialize(data, mask)
    }

    @Test
    fun `Date should be the same after serialization and deserialization`() {
        val data = Data(SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"))
        roundTripInlined(data)
        roundTrip(data)
    }

    @Test
    fun `different Dates should have same size after serialization`() {
        val data1 = Data(SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"))
        val data2 = Data(SimpleDateFormat("yyyy-MM-ddX").parse("2018-01-12+00"))

        sameSizeInlined(data1, data2)
        sameSize(data1, data2)
    }
}
