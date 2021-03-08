
import annotations.DFLength
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.junit.jupiter.api.Test
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

@ExperimentalSerializationApi
@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
class SerdeTest {
    companion object {
        val serializersModule = SerializersModule {
            polymorphic(PublicKey::class) {
                subclass(RSAPublicKeyImpl::class, RSAPublicKeySerializer)
            }
        }

        val serialNameRSAPublicKeyImpl = "RSAPublicKeyImpl"
    }

    @Test
    fun `serialize string`() {
        @Serializable
        data class Data(@DFLength([10]) val s: String = "123456789")
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
        @Serializable
        data class Data(@DFLength([10]) val s: String = "123456789")

        val data = Data()
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)

        assert(data == deserialized)
    }

    @Test
    fun `serialize list of string`() {
        @Serializable
        data class Data(@DFLength([2, 10]) val list: List<String> = listOf("123456789"))
        val mask = listOf(
            Pair("list.length", 4),
            Pair("string.length", 2),
            Pair("string.value", 2 * 10),
            Pair("string.length", 2),
            Pair("string.value", 2 * 10)
        )

        var data = Data()
        var bytes = checkedSerialize(data, mask)
        bytes[3].toInt() shouldBe data.list.size

        data = Data(listOf())
        bytes = checkedSerialize(data, mask)
        bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
    }

    @Test
    fun `serialize and deserialize list of string`() {
        @Serializable
        data class Data(@DFLength([2, 10]) val list: List<String> = listOf("123456789"))

        val data = Data()
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)

        assert(data == deserialized)
    }

    @Test
    fun `serialize 3rd party class`() {
        @Serializable
        data class Data(val date: @Serializable(with = DateSerializer::class) Date)
        val mask = listOf(Pair("date", 8))

        val data = Data(SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"))
        checkedSerialize(data, mask, DateSurrogate(Long.MIN_VALUE))
    }

    @Test
    fun `serialize and deserialize 3rd party class`() {
        @Serializable
        data class Data(val date: @Serializable(with = DateSerializer::class) Date)

        val data = Data(SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)

        assert(data == deserialized)
    }

    @Serializable
    data class Some(val i: Int)

    @Test
    fun `test of tests`() {
        @Serializable
        data class Data(
            @DFLength([3,   2])
            val map: Map<String, Some>
            )
        val mask = listOf(
            Pair("map.length", 4),
            Pair("map[0].key", 6),
            Pair("map[0].value", 4),
            Pair("map[1].key", 6),
            Pair("map[1].value", 4),
            Pair("map[2].key", 6),
            Pair("map[2].value", 4),
        )

        val data = Data(mapOf("a" to Some(1), "b" to Some(2)))
        checkedSerialize(data, mask)
    }

    @Test
    fun `test of tests with deserialization`() {
        @Serializable
        data class Data(
            @DFLength([3,   2])
            val map: Map<String, Some>
        )

        val data = Data(mapOf("a" to Some(1), "b" to Some(2)))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)

        assert(data == deserialized)
    }

    @Test
    fun `serialize list of 3rd party class`() {
        @Serializable
        data class Data(@DFLength([2]) val dates: List<@Serializable(with = DateSerializer::class) Date>)
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
        @Serializable
        data class Data(@DFLength([2]) val dates: List<@Serializable(with = DateSerializer::class) Date>)

        val data = Data(listOf(SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00")))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)

        assert(data == deserialized)
    }

    @Test
    fun `serialize deeply nested lists`() {
        @Serializable
        data class Data(
            @DFLength([  3,    4,   5]) 
            val nested: List<List<List<Int>>>)
        val mask = listOf(
            Pair("nested.length", 4),
            Pair("nested[0].length", 4),
            Pair("nested[0][0].length", 4),
            Pair("nested[0][0].value", 4 * 5),
            Pair("nested[0][1].length", 4),
            Pair("nested[0][1].value", 4 * 5),
            Pair("nested[0][2].length", 4),
            Pair("nested[0][2].value", 4 * 5),
            Pair("nested[0][3].length", 4),
            Pair("nested[0][3].value", 4 * 5),
            Pair("nested[1].length", 4),
            Pair("nested[1][0].length", 4),
            Pair("nested[1][0].value", 4 * 5),
            Pair("nested[1][1].length", 4),
            Pair("nested[1][1].value", 4 * 5),
            Pair("nested[1][2].length", 4),
            Pair("nested[1][2].value", 4 * 5),
            Pair("nested[1][3].length", 4),
            Pair("nested[1][3].value", 4 * 5),
            Pair("nested[2].length", 4),
            Pair("nested[2][0].length", 4),
            Pair("nested[2][0].value", 4 * 5),
            Pair("nested[2][1].length", 4),
            Pair("nested[2][1].value", 4 * 5),
            Pair("nested[2][2].length", 4),
            Pair("nested[2][2].value", 4 * 5),
            Pair("nested[2][3].length", 4),
            Pair("nested[2][3].value", 4 * 5)
        )

        var data = Data(listOf(listOf(listOf(2))))
        var bytes = checkedSerialize(data, mask)
        bytes.filter { it.toInt() != 0 }.sorted().distinct().toByteArray() shouldBe ByteArray(2) { (it + 1).toByte() }

        data = Data(listOf())
        bytes = checkedSerialize(data, mask)
        bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
    }

    @Test
    fun `serialize and deserialize deeply nested lists`() {
        @Serializable
        data class Data(
            @DFLength([  3,    4,   5])
            val nested: List<List<List<Int>>>)

        val data = Data(listOf(listOf(listOf(2))))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)

        assert(data == deserialized)
    }

    @Test
    fun `serialize compound type`() {
        @Serializable
        data class Data(val pair: Pair<Int, Int>)
        val mask = listOf(Pair("pair", 8))

        val data = Data(Pair(10, 20))
        checkedSerialize(data, mask)
    }

    @Test
    fun `serialize and deserialize compound type`() {
        @Serializable
        data class Data(val pair: Pair<Int, Int>)

        val data = Data(Pair(10, 20))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)

        assert(data == deserialized)
    }

    @Test
    fun `serialize list of compound type`() {
        @Serializable
        data class Data(@DFLength([2]) val list: List<Pair<Int, Int>>)
        val mask = listOf(
            Pair("list.length", 4),
            Pair("list[0]", 8),
            Pair("list[1]", 8),
        )

        var data = Data(listOf(Pair(10, 20)))
        var bytes = checkedSerialize(data, mask)
        bytes[3].toInt() shouldBe 1

        data = Data(listOf())
        bytes = checkedSerialize(data, mask)
        bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
    }

    @Test
    fun `serialize and deserialize list of compound type`() {
        @Serializable
        data class Data(@DFLength([2]) val list: List<Pair<Int, Int>>)

        val data = Data(listOf(Pair(10, 20)))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)

        assert(data == deserialized)
    }

    @Test
    fun `serialize list within a compound type`() {
        @Serializable
        data class Data(@DFLength([2]) val nested: Pair<Int, List<Int>>)
        val mask = listOf(
            Pair("pair.first", 4),
            Pair("pair.second.length", 4),
            Pair("pair.second.value", 8),
        )

        var data = Data(Pair(10, listOf(20)))
        @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
        var bytes = checkedSerialize(data, mask)

        data = Data(Pair(10, listOf()))
        bytes = checkedSerialize(data, mask)
        bytes shouldBe ByteArray(mask.sumBy { it.second }) { if (it == 3) { 10 } else { 0 } }
    }

    @Test
    fun `serialize and deserialize list within a compound type`() {
        @Serializable
        data class Data(@DFLength([2]) val nested: Pair<Int, List<Int>>)

        val data = Data(Pair(10, listOf(20)))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)

        assert(data == deserialized)
    }

    @Test
    fun `serialize list with own serializable class`() {
        @Serializable
        data class Data(@DFLength([2]) val list: List<Own>)
        val mask = listOf(
            Pair("list.length", 4),
            Pair("list[0].value", 4),
            Pair("list[1].value", 4),
        )

        var data = Data(listOf(Own()))
        @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
        var bytes = checkedSerialize(data, mask)

        data = Data(listOf())
        bytes = checkedSerialize(data, mask, Own())
        bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
    }

    @Test
    fun `serialize and deserialize list with own serializable class`() {
        @Serializable
        data class Data(@DFLength([2]) val list: List<Own>)

        val data = Data(listOf(Own()))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)

        assert(data == deserialized)
    }

    @Test
    fun `serialize list with own (with list) serializable class`() {
        val mask = listOf(
            Pair("own.list.length", 4),
            Pair("own.list[0].value", 4),
            Pair("own.list[1].value", 4),
        )

        var data = DataOwn(OwnList(listOf(10)))
        var bytes = checkedSerialize(data, mask)
        //
        data = DataOwn(OwnList(listOf()))
        bytes = checkedSerialize(data, mask, OwnList())
        bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
    }

    @Test
    fun `serialize and deserialize list with own (with list) serializable class`() {
        val data = DataOwn(OwnList(listOf(10)))
        val bytes = serialize(data)

        val deserialized: DataOwn = deserialize(bytes)

        assert(data == deserialized)
    }

    // I had to move out from the respective test, as otherwise it generates an error on the JVM level.
    @Serializable
    data class DataOwn(val own: OwnList)

    @Test
    fun `serialize plain map`() {
        @Serializable
        data class Data(@DFLength([2]) val map: Map<Int, Int>)
        val mask = listOf(
            Pair("map.length", 4),
            Pair("map[0].value", 8),
            Pair("map[1].value", 8),
        )

        var data = Data(mapOf(1 to 2))
        @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
        var bytes = checkedSerialize(data, mask)

        data = Data(mapOf())
        bytes = checkedSerialize(data, mask)
        bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
    }

    @Test
    fun `serialize and deserialize plain map`() {
        @Serializable
        data class Data(@DFLength([2]) val map: Map<Int, Int>)

        val data = Data(mapOf(1 to 2))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)

        assert(data == deserialized)
    }

    @Test
    fun `serialize complex map`() {
        @Serializable
        data class Data(@DFLength([2, 2, 2]) val map: Map<String, List<Int>>)
        val mask = listOf(
            Pair("map.length", 4),
            Pair("map[0].key", 2 + 2 * 2),
            Pair("map[0].value", 4 + 2 * 4),
            Pair("map[1].key", 2 + 2 * 2),
            Pair("map[1].value", 4 + 2 * 4),
        )

        var data = Data(mapOf("a" to listOf(2)))
        @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
        var bytes = checkedSerialize(data, mask)

        data = Data(mapOf())
        bytes = checkedSerialize(data, mask)
        bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
    }

    @Test
    fun `serialize and deserialize complex map`() {
        @Serializable
        data class Data(@DFLength([2, 2, 2]) val map: Map<String, List<Int>>)

        val data = Data(mapOf("a" to listOf(2)))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)

        assert(data == deserialized)
    }

    @Test
    fun `serialize complex map within a compound type`() {
        @Serializable
        data class Data(
            @DFLength([          2,          2,   2,     2])
            val nested: Triple<String, Int, Map<String, List<Int>>>)
        val mask = listOf(
            Pair("nested.1", 2 + 2 * 2),
            Pair("nested.2", 4),
            Pair("nested.3.length", 4),
            Pair("nested.3.map[0].key", 2 + 2 * 2),
            Pair("nested.3.map[0].value", 4 + 2 * 4),
            Pair("nested.3.map[1].key", 2 + 2 * 2),
            Pair("nested.3.map[1].value", 4 + 2 * 4),
        )

        var data = Data(Triple("a", 1, mapOf("a" to listOf(2))))
        checkedSerialize(data, mask)

        data = Data(Triple("a", 1, mapOf()))
        checkedSerialize(data, mask)
    }

    @Test
    fun `serialize and deserialize complex map within a compound type`() {
        @Serializable
        data class Data(
            @DFLength([          2,          2,   2,     2])
            val nested: Triple<String, Int, Map<String, List<Int>>>)

        val data = Data(Triple("a", 1, mapOf("a" to listOf(2))))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)

        assert(data == deserialized)
    }

    @Test
    fun `serialize polymorphic type itself`() {
        val mask = listOf(
            Pair("serialName", 2 + 2 * serialNameRSAPublicKeyImpl.length),
            Pair("length", 4),
            Pair("value", 500)
        )

        val data = getRSA()
        checkedSerialize(data, mask)
    }

    @Test
    fun `serialize and deserialize polymorphic type itself`() {
        val data = getRSA()
        val bytes = serialize(data)

        val deserialized: PublicKey = deserialize(bytes)

        assert(data == deserialized)
    }

    @Test
    fun `serialize polymorphic type within structure`() {
        @Serializable
        data class Data(val pk: PublicKey)

        val mask = listOf(
            Pair("pk.serialName", 2 + 2 * serialNameRSAPublicKeyImpl.length),
            Pair("pk.value", 4 + 500)
        )

        val data = Data(getRSA())
        checkedSerialize(data, mask)
    }

    @Test
    fun `serialize and deserialize polymorphic type within structure`() {
        @Serializable
        data class Data(val pk: PublicKey)

        val data = Data(getRSA())
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)

        assert(data == deserialized)
    }

    @Test
    fun `serialize polymorphic type within collection`() {
        @Serializable
        data class Data(@DFLength([2]) val nested: List<PublicKey>)

        val mask = listOf(
            Pair("nested.length", 4),
            Pair("nested[0].serialName", 2 + 2 * serialNameRSAPublicKeyImpl.length),
            Pair("nested[0].length", 4),
            Pair("nested[0].value", 500),
            Pair("nested[0].serialName", 2 + 2 * serialNameRSAPublicKeyImpl.length),
            Pair("nested[0].length", 4),
            Pair("nested[1].value", 500)
        )

        val data = Data(listOf(getRSA()))
        checkedSerialize(data, mask)
    }

    @Test
    fun `serialize and deserialize polymorphic type within collection`() {
        @Serializable
        data class Data(@DFLength([2]) val nested: List<PublicKey>)

        val data = Data(listOf(getRSA()))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)

        assert(data == deserialized)
    }

    @Test
    fun `serialize polymorphic type within nested compound type`() {
        @Serializable
        data class Some(val pk: PublicKey)

        @Serializable
        data class Data(val some: Some)

        val mask = listOf(
            Pair("some.pk.serialName", 2 + 2 * serialNameRSAPublicKeyImpl.length),
            Pair("some.pk.length", 4),
            Pair("some.nested.value", 500)
        )

        val data = Data(Some(getRSA()))
        checkedSerialize(data, mask)
    }

    @Test
    fun `serialize and deserialize polymorphic type within nested compound type`() {
        @Serializable
        data class Some(val pk: PublicKey)

        @Serializable
        data class Data(val some: Some)

        val data = Data(Some(getRSA()))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)

        assert(data == deserialized)
    }


    private inline fun <reified T: Any> checkedSerialize(data: T, mask: List<Pair<String, Int>>, vararg defaults: Any): ByteArray {
        val bytes = serialize(data, *defaults)
        log(bytes, mask)
        bytes.size shouldBe mask.sumBy { it.second }

        return bytes
    }

    private inline fun <reified T: Any> serialize(data: T, vararg defaults: Any): ByteArray {
        val output = ByteArrayOutputStream()
        val stream = DataOutputStream(output)
        encodeTo(stream, data, serializersModule, *defaults)
        return output.toByteArray()
    }

    private inline fun <reified T: Any> deserialize(data: ByteArray, vararg defaults: Any): T {
        val input = ByteArrayInputStream(data)
        val stream = DataInputStream(input)
        return decodeFrom(stream, serializersModule, *defaults)
    }

    private fun log(bytes: ByteArray, splitMask: List<Pair<String, Int>>) {
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
}

 fun getRSA(): PublicKey {
     val generator = KeyPairGenerator.getInstance("RSA")
     generator.initialize(2048, SecureRandom())
     return generator.genKeyPair().public
 }

