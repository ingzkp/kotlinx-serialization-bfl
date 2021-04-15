package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSizeInlined
import org.junit.jupiter.api.Test

class PrimitiveTypesTest {
    inline fun <reified T : Any> testPrimitive(value: T) {
        roundTripInlined(value, outerFixedLength = intArrayOf(5))
        roundTrip(value, outerFixedLength = intArrayOf(5))
    }

    @Test
    fun `direct serialization of primitive values should succeed`() {
        testPrimitive(true)
        testPrimitive(1.toByte())
        testPrimitive(1)
        testPrimitive(1L)
        testPrimitive('a')
    }

    @Test
    fun `direct serialization of floating point values should succeed`() {
        roundTripInlined(1.0F, outerFixedLength = intArrayOf(5, 5))
        roundTrip(1.0F, outerFixedLength = intArrayOf(5, 5))
        roundTripInlined(4.33, outerFixedLength = intArrayOf(5, 5))
        roundTrip(4.33, outerFixedLength = intArrayOf(5, 5))
    }

    @Test
    fun `direct serialization of a string should succeed`() {
        val string = "Test"
        roundTripInlined(string, outerFixedLength = intArrayOf(5))
        roundTrip(string, outerFixedLength = intArrayOf(5))

        val other = "What"
        sameSizeInlined(string, other, outerFixedLength = intArrayOf(5))
    }
}
