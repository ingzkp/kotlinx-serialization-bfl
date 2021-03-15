package zinc
import annotations.FixedLength
import com.ing.zknotary.common.zkp.ZincZKService
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import net.corda.core.contracts.Amount
import org.junit.jupiter.api.Test
import serde.SerdeTest
import serializers.AmountStringSerializer

import java.time.Duration

@ExperimentalSerializationApi
class AmountTest: SerdeTest() {

    @ExperimentalUnsignedTypes
    @Test
    fun `get max amount`() {
        @Serializable
        data class Data(
            @FixedLength([4]) val list: List<@Serializable(AmountStringSerializer::class) Amount<String>> = listOf(
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
        zincZKService.cleanup()
    }
}