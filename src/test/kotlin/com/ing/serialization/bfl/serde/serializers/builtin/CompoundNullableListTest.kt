package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import com.ing.serialization.bfl.api.reified.deserialize as deserializeInlined
import com.ing.serialization.bfl.api.reified.serialize as serializeInlined

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")

class CompoundNullableListTest {
    @Serializable
    data class NullableData(@FixedLength([2]) val nested: Pair<Int, List<Int>?>)

    @Test
    fun `serialize list within a compound type`() {
        val mask = listOf(
            Pair("pair.first", 4),
            Pair("pair.second.isNull", 1),
            Pair("pair.second.length", 4),
            Pair("pair.second.value", 8),
        )

        var data = NullableData(Pair(10, listOf(20)))
        checkedSerializeInlined(data, mask)

        data = NullableData(Pair(10, null))
        checkedSerializeInlined(data, mask)
    }

    @Test
    fun `serialize and deserialize list within a compound type`() {
        val data = NullableData(Pair(10, null))
        val bytes = serializeInlined(data)

        val deserialized: NullableData = deserializeInlined(bytes)
        data shouldBe deserialized
    }

    @Test
    fun `serialization has fixed length`() {
        val empty = Pair(0, listOf<Int>())
        val pair1 = Pair(1, listOf(1))
        val pair2 = Pair(2, listOf(1, 2))
        val pair3 = Pair(2, null)
        serializeInlined(NullableData(pair1)).size shouldBe serializeInlined(NullableData(pair2)).size
        serializeInlined(NullableData(pair2)).size shouldBe serializeInlined(NullableData(empty)).size
        serializeInlined(NullableData(pair1)).size shouldBe serializeInlined(NullableData(pair3)).size
    }
}
