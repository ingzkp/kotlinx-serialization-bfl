package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.deserialize
import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serialize
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

class NullableIntTest {
    @Serializable
    data class NullableData(val int: Int?)

    @Test
    fun `serialize nullable int`() {
        val mask = listOf(
            Pair("nonNull", 1),
            Pair("value", 4)
        )

        var data = NullableData(2)
        var bytes = checkedSerialize(data, mask)
        assert(bytes[0] != 0.toByte()) { "A non-null value is expected" }

        data = NullableData(null)
        bytes = checkedSerialize(data, mask)
        assert(bytes[0] == 0.toByte()) { "A null value is expected" }
    }

    @Test
    fun `serialize and deserialize nullable int`() {
        val data = NullableData(null)
        val bytes = serialize(data)

        val deserialized: NullableData = deserialize(bytes)
        data shouldBe deserialized
    }
}
