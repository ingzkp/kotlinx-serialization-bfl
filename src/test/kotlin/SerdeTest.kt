import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import serializers.RSAPublicKeySerializer
import sun.security.rsa.RSAPublicKeyImpl
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom

@ExperimentalSerializationApi
open class SerdeTest {
    companion object {
        val serializersModule = SerializersModule {
            polymorphic(PublicKey::class) {
                subclass(RSAPublicKeyImpl::class, RSAPublicKeySerializer)
            }
        }
    }

    inline fun <reified T : Any> checkedSerialize(
        data: T,
        mask: List<Pair<String, Int>>,
        vararg defaults: Any
    ): ByteArray {
        val bytes = serialize(data, *defaults)
        log(bytes, mask)
        bytes.size shouldBe mask.sumBy { it.second }

        return bytes
    }

    inline fun <reified T : Any> serialize(data: T, vararg defaults: Any): ByteArray {
        val output = ByteArrayOutputStream()
        val stream = DataOutputStream(output)
        encodeTo(stream, data, serializersModule, *defaults)
        return output.toByteArray()
    }

    inline fun <reified T : Any> deserialize(data: ByteArray, vararg defaults: Any): T {
        val input = ByteArrayInputStream(data)
        val stream = DataInputStream(input)
        return decodeFrom(stream, serializersModule, *defaults)
    }

    fun log(bytes: ByteArray, splitMask: List<Pair<String, Int>>) {
        println("Serialized:")
        // println("Raw: ${bytes.joinToString(separator = ",")}")
        var start = 0
        splitMask.forEach {
            val range = bytes.copyOfRange(start, start + it.second)
            val repr = range.joinToString(separator = ",") { d -> String.format("%2d", d) }
            println("${it.first} [$start, ${start + it.second}]\t: $repr")
            start += it.second
        }
    }

    fun getRSA(): PublicKey {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048, SecureRandom())
        return generator.genKeyPair().public
    }
}