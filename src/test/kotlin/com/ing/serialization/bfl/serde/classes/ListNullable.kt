package com.ing.serialization.bfl.serde.classes

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serde.SerdeTest
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
@ExperimentalSerializationApi
class ListNullable : SerdeTest() {
    @Serializable
    data class NullableData(@FixedLength([3]) val list: List<Int?>)

    @Test
    fun `serialize list with a primitive nullable type`() {
        val mask = listOf(
            Pair("list.length", 4),
            Pair("list[0]", 1 + 4),
            Pair("list[1]", 1 + 4),
            Pair("list[2]", 1 + 4),
        )

        val data = NullableData(listOf(25, null))
        checkedSerialize(data, mask)
    }

    @Test
    fun `serialize and deserialize list with a primitive nullable type`() {
        val data = NullableData(listOf(25, null))
        val bytes = serialize(data)

        val deserialized: NullableData = deserialize(bytes)
        data shouldBe deserialized
    }

    @Test
    fun `serialization has fixed length`() {
        val list1 = listOf(25)
        val list2 = listOf(null, null)
        serialize(NullableData(list1)).size shouldBe serialize(NullableData(list2)).size
        serialize(NullableData(list1)).size shouldBe serialize(NullableData(listOf())).size
    }
}
