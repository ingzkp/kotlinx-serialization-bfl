package com.ing.serialization.bfl.serde.classes

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.deserialize
import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serialize
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
@ExperimentalSerializationApi
@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
class StringNullableTest {
    @Serializable
    data class NullableData(@FixedLength([10]) val s: String? = "123456789")

    @Test
    fun `serialize nullable string`() {
        val mask = listOf(
            Pair("string.nonNull", 1),
            Pair("string.length", 2),
            Pair("string.value", 2 * 10)
        )

        var data = NullableData()
        var bytes = checkedSerialize(data, mask)
        assert(bytes[0] != 0.toByte()) { "A non-null value is expected" }
        bytes[2].toInt() shouldBe data.s?.length

        data = NullableData(null)
        bytes = checkedSerialize(data, mask)
        assert(bytes[0] == 0.toByte()) { "A null value is expected" }
    }

    @Test
    fun `serialize and deserialize nullable string`() {
        val data = NullableData(null)
        val bytes = serialize(data)

        val deserialized: NullableData = deserialize(bytes)
        data shouldBe deserialized
    }
}
