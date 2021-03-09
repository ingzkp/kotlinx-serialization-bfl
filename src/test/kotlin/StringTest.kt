import annotations.DFLength
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom

@ExperimentalSerializationApi
@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
class StringTest: SerdeTest() {
    @Serializable
    data class Data(@DFLength([10]) val s: String = "123456789")

    @Test
    fun `serialize string`() {
        val mask = listOf(
            Pair("string.length", 2),
            Pair("string.value", 2 * 10)
        )

        var data = Data()
        var bytes = checkedSerialize(data, mask)
        bytes[1].toInt() shouldBe data.s.length

        data = Data("")
        bytes = checkedSerialize(data, mask)
        bytes[1].toInt() shouldBe data.s.length
    }

    @Test
    fun `serialize and deserialize string`() {
        val data = Data()
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)
        data shouldBe deserialized
    }

    @Test
    fun `serialization has fixed length`() {
        serialize(Data("1")).size shouldBe serialize(Data("12")).size
    }
}
