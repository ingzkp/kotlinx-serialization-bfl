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

class MapTest {
    @Serializable
    data class Data(@FixedLength([2]) val map: Map<Int, Int>)

    @Test
    fun `plain Map should be serialized successfully`() {
        val mask = listOf(
            Pair("map.length", 4),
            Pair("map[0].value", 8),
            Pair("map[1].value", 8),
        )

        var data = Data(mapOf(1 to 2))
        checkedSerializeInlined(data, mask)
        checkedSerialize(data, mask)

        data = Data(mapOf())
        listOf(
            checkedSerializeInlined(data, mask),
            checkedSerialize(data, mask),
        ).forEach { bytes ->
            bytes shouldBe ByteArray(mask.sumBy { it.second }) { 0 }
        }
    }

    @Test
    fun `plain Map should be the same after serialization and deserialization`() {
        val data = Data(mapOf(1 to 2))

        roundTripInlined(data)
        roundTrip(data)
    }

    @Test
    fun `different plain Maps should have same size after serialization`() {
        val empty = Data(mapOf())
        val data1 = Data(mapOf(1 to 2))
        val data2 = Data(mapOf(1 to 2, 2 to 3))

        sameSizeInlined(empty, data1)
        sameSize(empty, data1)
        sameSizeInlined(data2, data1)
        sameSize(data2, data1)
    }
}
