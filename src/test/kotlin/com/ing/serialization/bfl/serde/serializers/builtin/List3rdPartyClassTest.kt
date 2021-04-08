package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.serialize
import com.ing.serialization.bfl.serde.SerdeError
import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import com.ing.serialization.bfl.serializers.DateSerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.text.SimpleDateFormat
import java.util.Date

class List3rdPartyClassTest {
    @Serializable
    data class Data(@FixedLength([2]) val dates: List<@Serializable(with = DateSerializer::class) Date>)

    @Test
    fun `List of 3rd party class should be serialized successfully`() {
        val mask = listOf(
            Pair("dates.length", 4),
            Pair("dates[0]", 8),
            Pair("dates[1]", 8),
        )

        var data = Data(listOf(SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00")))
        listOf(
            checkedSerializeInlined(data, mask),
            checkedSerialize(data, mask),
        ).forEach { bytes ->
            bytes[3].toInt() shouldBe data.dates.size
        }

        data = Data(listOf())
        listOf(
            checkedSerializeInlined(data, mask),
            checkedSerialize(data, mask),
        ).forEach { bytes ->
            bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
        }
    }

    @Test
    fun `List of 3rd party class should be the same after serialization and deserialization`() {
        val data = Data(listOf(SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00")))

        roundTripInlined(data)
        roundTrip(data)
    }

    @Test
    fun `different Lists of 3rd party class should have same size after serialization`() {
        val date1 = SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00")
        val date2 = SimpleDateFormat("yyyy-MM-ddX").parse("2017-02-15+00")

        val empty = Data(listOf())
        val data1 = Data(listOf(date1))
        val data2 = Data(listOf(date1, date2))

        sameSizeInlined(empty, data1)
        sameSize(empty, data1)
        sameSizeInlined(data2, data1)
        sameSize(data2, data1)
    }

    @Test
    fun `too long List of 3rd party class should throw CollectionTooLarge`() {
        assertThrows<SerdeError.CollectionTooLarge> {
            serialize(
                Data(
                    listOf(
                        SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"),
                        SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"),
                        SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00")
                    )
                )
            )
        }
    }
}
