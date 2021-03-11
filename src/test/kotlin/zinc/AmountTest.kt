package zinc
import annotations.DFLength
import com.ing.zknotary.common.zkp.ZincZKService
import encodeTo
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.EmptySerializersModule
import net.corda.core.contracts.Amount
import org.junit.jupiter.api.Test
import serializers.AmountStringSerializer
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.time.Duration

@ExperimentalSerializationApi
class AmountTest {

    @ExperimentalUnsignedTypes
    @Test
    fun `get max amount`() {
        @Serializable
        data class Data(
            @DFLength([4]) val list: List<@Serializable(AmountStringSerializer::class) Amount<String>> = listOf(
                Amount(100, "some.token"),
                Amount(250, "some.token"),
                Amount(220, "some.token"),
            )
        )
        val mask = listOf(
            Pair("list.length", 4),
            Pair("list.[0].quantity", 8),
            Pair("list.[0].display_token_size.sign", 1),
            Pair("list.[0].display_token_size.integer.length", 4),
            Pair("list.[0].display_token_size.integer.value", 100),
            Pair("list.[0].display_token_size.fraction.length", 4),
            Pair("list.[0].display_token_size.fraction.value", 20),
            Pair("list.[0].token", 4 + 32),
            Pair("list.[1].quantity", 8),
            Pair("list.[1].display_token_size.sign", 1),
            Pair("list.[1].display_token_size.integer.length", 4),
            Pair("list.[1].display_token_size.integer.value", 100),
            Pair("list.[1].display_token_size.fraction.length", 4),
            Pair("list.[1].display_token_size.fraction.value", 20),
            Pair("list.[1].token", 4 + 32),
            Pair("list.[2].quantity", 8),
            Pair("list.[2].display_token_size.sign", 1),
            Pair("list.[2].display_token_size.integer.length", 4),
            Pair("list.[2].display_token_size.integer.value", 100),
            Pair("list.[2].display_token_size.fraction.length", 4),
            Pair("list.[2].display_token_size.fraction.value", 20),
            Pair("list.[2].token", 4 + 32),
            Pair("list.[3].quantity", 8),
            Pair("list.[3].display_token_size.sign", 1),
            Pair("list.[3].display_token_size.integer.length", 4),
            Pair("list.[3].display_token_size.integer.value", 100),
            Pair("list.[3].display_token_size.fraction.length", 4),
            Pair("list.[3].display_token_size.fraction.value", 20),
            Pair("list.[3].token", 4 + 32)
        )

        val data = Data()
        val bytes = checkedSerialize(data, mask)

        val circuitFolder: String = AmountTest::class.java.getResource("/MaxAmountDemo").path
        val zincZKService = ZincZKService(
            circuitFolder,
            artifactFolder = circuitFolder,
            buildTimeout = Duration.ofSeconds(5),
            setupTimeout = Duration.ofSeconds(300),
            provingTimeout = Duration.ofSeconds(300),
            verificationTimeout = Duration.ofSeconds(1)
        )

        zincZKService.setup()
        println(zincZKService.run(bytes))
    }

    @ExperimentalUnsignedTypes
    private inline fun <reified T: Any> checkedSerialize(data: T, mask: List<Pair<String, Int>>): ByteArray {
        val bytes = serialize(data)
        log(bytes, mask)
        bytes.size shouldBe mask.sumBy { it.second }

        return bytes
    }

    private inline fun <reified T: Any> serialize(data: T): ByteArray {
        val output = ByteArrayOutputStream()
        val stream = DataOutputStream(output)
        encodeTo(stream, data, EmptySerializersModule)
        return output.toByteArray()
    }

    @ExperimentalUnsignedTypes
    private fun log(bytes: ByteArray, splitMask: List<Pair<String, Int>>) {
        val ubytes = bytes.map { it.toUByte() }
        println("Serialized:")
        // println("Raw: ${bytes.joinToString(separator = ",")}")
        var start = 0
        splitMask.forEach {
            val range = ubytes.subList(start, start + it.second)
            val repr = range.joinToString(separator = ",")
            println("${it.first} [$start, ${start + it.second - 1}]\t: $repr")
            start += it.second
        }
    }
}