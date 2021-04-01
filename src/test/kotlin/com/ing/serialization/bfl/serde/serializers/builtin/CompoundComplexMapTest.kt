package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")

class CompoundComplexMapTest {
    @Serializable
    data class Data(
        @FixedLength([2, 2, 2, 2])
        val nested: Triple<String, Int, Map<String, List<Int>>>
    )

    @Test
    fun `serialize complex map within a compound type`() {
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
        checkedSerializeInlined(data, mask)

        data = Data(Triple("a", 1, mapOf()))
        checkedSerializeInlined(data, mask)
    }

    @Test
    fun `serialize and deserialize complex map within a compound type`() {
        val data = Data(Triple("a", 1, mapOf("a" to listOf(2))))

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `serialization has fixed length`() {
        val empty = Data(Triple("", 0, mapOf()))
        val data1 = Data(Triple("a", 1, mapOf("a" to listOf(2))))
        val data2 = Data(Triple("ba", 2, mapOf("ah" to listOf(2, 4))))

        sameSizeInlined(empty, data1)
        sameSize(empty, data1)
        sameSizeInlined(data2, data1)
        sameSize(data2, data1)
    }
}
