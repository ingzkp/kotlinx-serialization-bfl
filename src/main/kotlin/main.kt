import annotations.FixedLength
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.text.SimpleDateFormat
import java.util.Date

@ExperimentalUnsignedTypes
@ExperimentalSerializationApi
fun main() {
    val data = ML(
        "12345678901234567890",
        listOf(),
        listOf(Pair(1, 2)),
        SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"),
        listOf(Own(25))
    )
    val splitMask = listOf(
        Pair("string", 2 + 2 * 20),
        Pair("dates", 4 + 3 * 8),
        Pair("pairs", 4 + 2 * (4 + 4)),
        Pair("date", 8),
        Pair("owns", 4 + 2 * 4)
    )
    println(data)

    val output = ByteArrayOutputStream()
    encodeTo(DataOutputStream(output), data, Own(), DateSurrogate(Long.MIN_VALUE))
    val bytes = output.toByteArray()

    println("Serialized:")
    var start = 0
    splitMask.forEach {
        val range = bytes.copyOfRange(start, start + it.second)
        val repr = range.joinToString(separator = ",") { d -> String.format("%2d", d) }
        // val repr = range.toAsciiHexString()
        println("${it.first}\t: $repr")
        start += it.second
    }

    val deserialized = decodeFrom<ML>(DataInputStream(ByteArrayInputStream(bytes)))
    println(deserialized)
}

@Serializable
@ExperimentalSerializationApi
data class ML(
    @FixedLength(20)
    val string: String,

    @FixedLength(3)
    val dates: List<@Serializable(with = DateSerializer::class) Date>,

    @FixedLength(2)
    val pairs: List<Pair<Int, Int>>,

    @Serializable(with = DateSerializer::class)
    val date: Date,

    @FixedLength(2)
    val owns: List<Own>
)