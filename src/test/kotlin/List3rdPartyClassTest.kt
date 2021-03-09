import annotations.DFLength
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat
import java.util.Date

@ExperimentalSerializationApi
class List3rdPartyClassTest: SerdeTest() {
    @Serializable
    data class Data(@DFLength([2]) val dates: List<@Serializable(with = DateSerializer::class) Date>)

    @Test
    fun `serialize list of 3rd party class`() {
        val mask = listOf(
            Pair("dates.length", 4),
            Pair("dates[0]", 8),
            Pair("dates[1]", 8),
        )

        var data = Data(listOf(SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00")))
        var bytes = checkedSerialize(data, mask, DateSurrogate(Long.MIN_VALUE))
        bytes[3].toInt() shouldBe data.dates.size

        data = Data(listOf())
        bytes = checkedSerialize(data, mask, DateSurrogate(Long.MIN_VALUE))
        bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
    }

    @Test
    fun `serialize and deserialize list of 3rd party class`() {
        val data = Data(listOf(SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00")))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)
        data shouldBe deserialized
    }

    @Test
    fun `serialization has fixed length`() {
        val date1 = SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00")
        val date2 = SimpleDateFormat("yyyy-MM-ddX").parse("2017-02-15+00")
        serialize(Data(listOf(date1))).size shouldBe serialize(Data(listOf(date1, date2))).size
        serialize(Data(listOf(date1))).size shouldBe serialize(Data(listOf())).size
    }
}