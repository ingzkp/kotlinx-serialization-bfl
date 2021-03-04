import annotations.DFLength
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.junit.jupiter.api.Test
import serializers.RSAPublicKeySerializer
import sun.security.rsa.RSAPublicKeyImpl
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.PublicKey

@ExperimentalSerializationApi
class SerdeTest {
    companion object {
        val serializersModule = SerializersModule {
            polymorphic(PublicKey::class) {
                subclass(RSAPublicKeyImpl::class, RSAPublicKeySerializer)
            }
        }
    }

    private inline fun <reified T: Any> serialize(data: T, vararg defaults: Any): ByteArray{
        val output = ByteArrayOutputStream()
        val stream = DataOutputStream(output)
        encodeTo(stream, data, SerdeTest.serializersModule, *defaults)
        return output.toByteArray()
    }


    private fun log(bytes: ByteArray, splitMask: List<Pair<String, Int>>) {
        println("Serialized:")
        var start = 0
        splitMask.forEach {
            val range = bytes.copyOfRange(start, start + it.second)
            val repr = range.joinToString(separator = ",") { d -> String.format("%2d", d) }
            println("${it.first} [$start, ${start + it.second}]\t: $repr")
            start += it.second
        }
    }

    @Test
    fun `serialize string`() {
        @Serializable
        data class Data(@DFLength([10]) val s: String = "123456789")

        val bytes = serialize(Data())
        log(bytes, listOf(Pair("string.length", 2), Pair("string.value", 2 * 10),))

        bytes.size shouldBe 22
        bytes[1] shouldBe 9
    }
}


//
// @Serializable
// @ExperimentalSerializationApi
// data class CoverAll(
//     @ValueLength([25])
//     val string: String,
//
//     @ValueLength([3])
//     val dates: List<@Serializable(with = DateSerializer::class) Date>,
//
//     @ValueLength([3, 4, 5])
//     val listMatrix: List<List<List<Int>>>,
//
//     @ValueLength([2])
//     val pairs: List<Pair<Int, Int>>,
//
//     @Serializable(with = DateSerializer::class)
//     val date: Date,
//
//     @ValueLength([2])
//     val owns: List<Own>,
//
//     @Serializable(with = RSAPublicKeySerializer::class)
//     val publicKey: PublicKey,
//
//     @KeyLength([2])
//     @ValueLength([4])
//     val map: Map<String, List<Int>>
//
//     // Empty List of Strings. I expect it to fail because it does not set lastStructureSize.
// )