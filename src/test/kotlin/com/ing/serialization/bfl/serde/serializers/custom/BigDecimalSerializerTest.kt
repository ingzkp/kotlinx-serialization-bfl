package com.ing.serialization.bfl.serde.serializers.custom

import com.ing.serialization.bfl.api.serialize
import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import com.ing.serialization.bfl.serializers.BFLSerializers
import com.ing.serialization.bfl.serializers.BigDecimalSurrogate
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class BigDecimalSerializerTest {
    @Serializable
    data class Data(val value: @Contextual BigDecimal)

    @Test
    fun `BigDecimalSurrogate should convert to BigDecimal`() {
        listOf(
            Pair(
                Triple(1.toByte(), byteArrayOf(4), byteArrayOf(3, 3)),
                4.33
            ),
            Pair(
                Triple((-1).toByte(), byteArrayOf(4), byteArrayOf(3, 3)),
                -4.33
            ),

        ).forEach {
            BigDecimalSurrogate(
                it.first.first,
                ByteArray(BigDecimalSurrogate.INTEGER_SIZE - it.first.second.size) { 0 } + it.first.second,
                it.first.third + ByteArray(BigDecimalSurrogate.FRACTION_SIZE - it.first.third.size) { 0 }
            ).toOriginal() shouldBe it.second.toBigDecimal()
        }
    }

    @Test
    fun `BigDecimalSurrogate init should throw IllegalArgumentException when integer size is incorrect`() {
        assertThrows<IllegalArgumentException> {
            BigDecimalSurrogate(
                1.toByte(),
                byteArrayOf(4),
                byteArrayOf(3, 3)
            )
        }.also {
            it.message shouldBe "Integer part must have size no longer than ${BigDecimalSurrogate.INTEGER_SIZE}, but has ${byteArrayOf(4).size}"
        }
    }

    @Test
    fun `BigDecimalSurrogate init should throw IllegalArgumentException when fraction size is incorrect`() {
        assertThrows<IllegalArgumentException> {
            BigDecimalSurrogate(
                1.toByte(),
                ByteArray(BigDecimalSurrogate.INTEGER_SIZE - byteArrayOf(4).size) { 0 } + byteArrayOf(4),
                byteArrayOf(3, 3)
            )
        }.also {
            it.message shouldBe "Fraction part must have size no longer than ${BigDecimalSurrogate.FRACTION_SIZE}, but has ${byteArrayOf(3, 3).size}"
        }
    }

    @Test
    fun `BigDecimal should be serialized successfully`() {
        val mask = listOf(
            Pair("sign", 1),
            Pair("integer", 4 + BigDecimalSurrogate.INTEGER_SIZE),
            Pair("fraction", 4 + BigDecimalSurrogate.FRACTION_SIZE)
        )

        val data = Data(4.33.toBigDecimal())
        checkedSerializeInlined(data, mask)
        checkedSerialize(data, mask)
    }

    @Test
    fun `BigDecimal should be the same after serialization and deserialization`() {
        listOf(
            Data(4.33.toBigDecimal()),
            Data((-4.33).toBigDecimal()),
            Data(BigDecimal.TEN),
            Data(BigDecimal("0.01"))
        ).forEach { data ->
            roundTripInlined(data)
            roundTrip(data)
        }
    }

    @Test
    fun `different BigDecimals should have same size after serialization`() {
        val data1 = Data(4.33.toBigDecimal())
        val maxBigDecimal = BigDecimal("${"9".repeat(BigDecimalSurrogate.INTEGER_SIZE)}.${"9".repeat(BigDecimalSurrogate.FRACTION_SIZE)}")
        val data2 = Data(maxBigDecimal)

        sameSizeInlined(data1, data2)
        sameSize(data1, data2)
    }

    @Test
    fun `serialize BigDecimal should throw IllegalArgumentException when integer size limit is not respected`() {
        val integerOverSized = BigDecimal("${"9".repeat(BigDecimalSurrogate.INTEGER_SIZE + 1)}.${"9".repeat(BigDecimalSurrogate.FRACTION_SIZE)}")

        assertThrows<IllegalArgumentException> {
            serialize(Data(integerOverSized), BFLSerializers)
        }.also {
            it.message shouldBe "BigDecimal supports no more than ${BigDecimalSurrogate.INTEGER_SIZE} digits in integer part " +
                "and ${BigDecimalSurrogate.FRACTION_SIZE} digits in fraction part"
        }
    }

    @Test
    fun `serialize BigDecimal should throw IllegalArgumentException when fraction size limit is not respected`() {
        val fractionOverSized = BigDecimal("${"9".repeat(BigDecimalSurrogate.INTEGER_SIZE)}.${"9".repeat(BigDecimalSurrogate.FRACTION_SIZE + 1)}")

        assertThrows<IllegalArgumentException> {
            serialize(Data(fractionOverSized), BFLSerializers)
        }.also {
            it.message shouldBe "BigDecimal supports no more than ${BigDecimalSurrogate.INTEGER_SIZE} digits in integer part " +
                "and ${BigDecimalSurrogate.FRACTION_SIZE} digits in fraction part"
        }
    }
}
