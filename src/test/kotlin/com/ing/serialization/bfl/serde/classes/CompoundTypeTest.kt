package com.ing.serialization.bfl.serde.classes

import com.ing.serialization.bfl.serde.SerdeTest
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

@ExperimentalSerializationApi
class CompoundTypeTest : SerdeTest() {
    @Serializable
    data class Data(val pair: Pair<Int, Int>)

    @Test
    fun `serialize compound type`() {
        val mask = listOf(Pair("pair", 8))

        val data = Data(Pair(10, 20))
        checkedSerialize(data, mask)
    }

    @Test
    fun `serialize and deserialize compound type`() {
        val data = Data(Pair(10, 20))
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)
        data shouldBe deserialized
    }

    @Test
    fun `serialization has fixed length`() {
        serialize(Data(Pair(1, 2))).size shouldBe serialize(Data(Pair(-3, -4))).size
    }
}
