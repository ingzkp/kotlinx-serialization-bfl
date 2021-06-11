package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.api.reified.deserialize
import com.ing.serialization.bfl.api.serialize
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serializers.BFLSerializers
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ObjectTest {
    @Serializable
    object StatelessObject {
        const val myInt = 0 // constants should not produce any problems during serialization
    }

    @Serializable
    object NestedStatelessObject {
        val myObject = StatelessObject
    }

    @Serializable
    object StatefulObject {
        var myInt = 0
    }

    @Serializable
    data class StatelessNullData(val value: StatelessObject?)

    @Serializable
    data class StatefulNullData(val value: StatefulObject?)

    companion object {
        @JvmStatic
        fun statelessObjects() = listOf(
            StatelessObject,
            NestedStatelessObject
        )

        @JvmStatic
        fun nullObjectData() = listOf(
            StatelessNullData(null),
            StatefulNullData(null)
        )
    }

    @ParameterizedTest
    @MethodSource("statelessObjects")
    fun `Object should be serialized successfully`(statelessObject: Any) {
        serialize(statelessObject).size shouldBe 0
    }

    @ParameterizedTest
    @MethodSource("statelessObjects")
    fun `Object should be the same after serialization and deserialization`(statelessObject: Any) {
        roundTripInlined(StatelessObject)
        roundTripInlined(NestedStatelessObject)

        roundTrip(statelessObject)
    }

    @Test
    fun `serialization of Object with mutable properties should ignore serialized mutable state`() {
        val ser = serialize(StatefulObject)

        // Mutate state after serialization
        StatefulObject.myInt = 123

        val obj = deserialize<StatefulObject>(ser, serializersModule = BFLSerializers)

        // Deserialization should not mutate singleton state
        obj.myInt shouldBe 123
        obj shouldBeSameInstanceAs StatefulObject
    }

    @ParameterizedTest
    @MethodSource("nullObjectData")
    fun `serialization of null objects should succeed regardless of the mutability of its properties`(nullObjectData: Any) {
        assertDoesNotThrow { serialize(nullObjectData) }
    }
}
