package serde

import DateSerializer
import DateSurrogate
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@ExperimentalSerializationApi
class ThirdPartyClassTest: SerdeTest() {
    @Serializable
    data class Data(val date: @Serializable(with = DateSerializer::class) Date)

    @Test
    fun `serialize 3rd party class`() {
        val mask = listOf(Pair("date", 8))

        val data = Data(SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"))
        checkedSerialize(data, mask)
    }

    @Test
    fun `serialize and deserialize 3rd party class`() {
        val data = Data(SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)
        data shouldBe deserialized
    }
}