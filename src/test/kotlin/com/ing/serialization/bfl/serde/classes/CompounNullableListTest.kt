package com.ing.serialization.bfl.serde.classes

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serde.SerdeTest
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
@ExperimentalSerializationApi
class CompoundNullableListTest : SerdeTest() {
    @Serializable
    data class NullableData(@FixedLength([2]) val nested: Pair<Int, List<Int>>?)

    @Test
    fun `serialize list within a nullable compound type`() {
        val mask = listOf(
            Pair("pair.nonNull", 1),
            Pair("pair.first", 4),
            Pair("pair.second.length", 4),
            Pair("pair.second.value", 8),
        )

        var data = NullableData(Pair(10, listOf(20)))
        var bytes = checkedSerialize(data, mask)
        assert(bytes[0] != 0.toByte()) { "A non-null value is expected" }

        data = NullableData(null)
        bytes = checkedSerialize(data, mask)
        assert(bytes[0] == 0.toByte()) { "A null value is expected" }
    }

    @Test
    fun `serialize and deserialize list within a nullable compound type`() {
        val data = NullableData(null)
        val bytes = serialize(data)

        val deserialized: NullableData = deserialize(bytes)
        data shouldBe deserialized
    }
}
