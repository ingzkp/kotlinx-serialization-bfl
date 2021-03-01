import annotations.FixedLength
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
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
import java.text.SimpleDateFormat
import java.util.*

@ExperimentalUnsignedTypes
@ExperimentalSerializationApi
fun main() {
    val data = CoverAll(
        "12345678901234567890",
        listOf(),
        listOf(listOf(1, 2, 3), listOf(4, 5, 6)),
        listOf(Pair(1, 2)),
        SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"),
        listOf(Own(25)),
        getRSA()
    )
    val splitMask = listOf(
        Pair("string.length", 2),
        Pair("string.value", 2 * 25),
        Pair("dates.length", 4),
        Pair("dates.value", 8 * 3),
        Pair("listMatrix.length", 4),
        Pair("listMatrix[0].length", 4),
        Pair("listMatrix[0].value", 4 * 5),
        Pair("listMatrix[1].length", 4),
        Pair("listMatrix[1].value", 4 * 5),
        Pair("listMatrix[2].length", 4),
        Pair("listMatrix[2].value", 4 * 5),
        Pair("listMatrix[3].length", 4),
        Pair("listMatrix[3].value", 4 * 5),
        Pair("pairs.length", 4),
        Pair("pairs.value", 2 * (4 + 4)),
        Pair("date", 8),
        Pair("owns.length", 4),
        Pair("owns.value", 2 * 4),
        Pair("publicKey.length", 4),
        Pair("publicKey.length", 500)
    )
    println(data)

    val output = ByteArrayOutputStream()
    val serializersModule = SerializersModule {
        polymorphic(PublicKey::class) {
            subclass(RSAPublicKeyImpl::class, RSAPublicKeySerializer)
        }
    }
    encodeTo(DataOutputStream(output), data, serializersModule, Own(), DateSurrogate(Long.MIN_VALUE))
    val bytes = output.toByteArray()

    println("Serialized:")
    var start = 0
    splitMask.forEach {
        val range = bytes.copyOfRange(start, start + it.second)
        val repr = range.joinToString(separator = ",") { d -> String.format("%2d", d) }
        // val repr = range.toAsciiHexString()
        println("${it.first} [$start, ${start + it.second}]\t: $repr")
        start += it.second
    }

    val deserialized = decodeFrom<CoverAll>(DataInputStream(ByteArrayInputStream(bytes)))
    println(deserialized)

    // val data = Outer()
    // val output = ByteArrayOutputStream()
    // encodeTo(DataOutputStream(output), data)
    // val bytes = output.toByteArray()
    // println("Serialized: ${bytes.joinToString(separator = ",")}")
    // val deserialized = decodeFrom<Outer>(DataInputStream(ByteArrayInputStream(bytes)))
    // println(deserialized)
}

@Serializable
@ExperimentalSerializationApi
data class CoverAll(
    @FixedLength([25])
    val string: String,

    @FixedLength([3])
    val dates: List<@Serializable(with = DateSerializer::class) Date>,

    @FixedLength([4, 5])
    val listMatrix: List<List<Int>>,

    @FixedLength([2])
    val pairs: List<Pair<Int, Int>>,

    @Serializable(with = DateSerializer::class)
    val date: Date,

    @FixedLength([2])
    val owns: List<Own>,

    @Serializable(with = RSAPublicKeySerializer::class)
    val publicKey: PublicKey
)

@Serializable
data class Outer(
    // Shows necessity for stack.
    // @FixedLength(2)
    // val ints1: List<Int>,
    // @FixedLength(2)
    // val ints2: List<Int>,

    // @FixedLength(2)
    // val ints_o: List<Inner> = listOf(Inner())

    @FixedLength([2, 3])
    val ints: List<List<Int>> = listOf(listOf(100))
)

@Serializable
data class Inner(
    @FixedLength([3])
    val ints_i: List<Int> = listOf(100)
)

fun getRSA(): PublicKey {
    val generator = KeyPairGenerator.getInstance("RSA")
    generator.initialize(2048, SecureRandom())
    return generator.genKeyPair().public
}