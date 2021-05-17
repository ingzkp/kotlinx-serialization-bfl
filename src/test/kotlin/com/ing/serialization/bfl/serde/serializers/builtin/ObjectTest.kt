package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.api.serialize
import com.ing.serialization.bfl.serde.SerdeError
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class ObjectTest {
    @Serializable
    object StatelessObject {
        const val myInt = 0     // constants should not produce any problems during serialization
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
    object NestedStatefulObject {
        val myObject = StatefulObject
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
        fun statefulObjects() = listOf(
            StatefulObject,
            NestedStatefulObject
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

    @ParameterizedTest
    @MethodSource("statefulObjects")
    fun `serialization of Object with mutable properties should fail`(statefulObject: Any) {
        assertThrows<SerdeError.MutablePropertiesInObject> { serialize(statefulObject) }
    }

    @ParameterizedTest
    @MethodSource("nullObjectData")
    fun `serialization of null objects should succeed regardless of the mutability of its properties`(nullObjectData: Any) {
        assertDoesNotThrow { serialize(nullObjectData) }
    }
}
