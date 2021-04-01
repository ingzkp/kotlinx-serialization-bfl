package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
class StringTest {
    @Serializable
    data class Data(@FixedLength([10]) val s: String = "123456789")

    @Test
    fun `serialize string`() {
        val mask = listOf(
            Pair("string.length", 2),
            Pair("string.value", 2 * 10)
        )

        var data = Data()
        var bytes = checkedSerializeInlined(data, mask)
        bytes[1].toInt() shouldBe data.s.length
        bytes = checkedSerialize(data, mask)
        bytes[1].toInt() shouldBe data.s.length

        data = Data("")
        bytes = checkedSerializeInlined(data, mask)
        bytes[1].toInt() shouldBe data.s.length
        bytes = checkedSerialize(data, mask)
        bytes[1].toInt() shouldBe data.s.length
    }

    @Test
    fun `serialize and deserialize string`() {
        val data = Data()

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `serialization has fixed length`() {
        val data1 = Data("1")
        val data2 = Data("12")

        sameSizeInlined(data1, data2)
        sameSize(data1, data2)
    }
}
