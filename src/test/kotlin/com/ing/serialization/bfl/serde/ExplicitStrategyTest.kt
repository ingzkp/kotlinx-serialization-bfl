package com.ing.serialization.bfl.serde

import com.ing.serialization.bfl.annotations.FixedLength
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import org.junit.jupiter.api.Test

class ExplicitStrategyTest {
    @Serializable
    data class Base<T>(@FixedLength([10]) val value: T)

    @Test
    fun `explicit strategy passing works`() {
        val str = Base("Works")

        roundTripInlined(str, strategy = Base.serializer(String.serializer()))
        roundTrip(str, strategy = Base.serializer(String.serializer()))

        val int = Base(433)

        roundTripInlined(int, strategy = Base.serializer(Int.serializer()))
        roundTrip(int, strategy = Base.serializer(Int.serializer()))
    }
}
