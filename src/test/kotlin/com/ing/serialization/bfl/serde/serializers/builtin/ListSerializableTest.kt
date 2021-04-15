package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSizeInlined
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

class ListSerializableTest {
    @Test
    fun `List with primitive should be serialized directly`() {
        val list = listOf(1)

        roundTripInlined(list, outerFixedLength = intArrayOf(2))
        // roundTrip will fail because of type information loss,
        // see comments in Api.kt

        sameSizeInlined(listOf(1, 2), listOf(1), outerFixedLength = intArrayOf(2))
    }

    @Serializable
    data class Data(val int: Int = 2)

    @Test
    fun `List with serializable class should be serialized directly`() {
        val list = listOf(Data())

        roundTripInlined(list, outerFixedLength = intArrayOf(2))
        // roundTrip will fail because of type information loss,
        // see comments in Api.kt

        sameSizeInlined(listOf(Data(1)), listOf(Data(1), Data(2)), outerFixedLength = intArrayOf(2))
    }
}
