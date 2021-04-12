package com.ing.serialization.bfl.serde.serializers.custom

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.reified.serialize
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class BigDecimalDocTest {
    @Serializable
    data class Data(
        @FixedLength([6, 4])
        val value: @Contextual BigDecimal
    )

    @Test
    fun `verify encoded format`() {
        val data = Data(BigDecimal("123.456"))
        val serializedBytes = serialize(data)
        val serializedString = serializedBytes.joinToString(", ")
        serializedString shouldBe "1, 0, 0, 0, 3, 3, 2, 1, 0, 0, 0, 0, 0, 0, 3, 4, 5, 6, 0"
    }
}
