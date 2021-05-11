package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.api.serialize
import com.ing.serialization.bfl.serde.SerdeError
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class ObjectTest {
    @Serializable
    object StatelessObject {
        val myInt = 0
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

    @Test
    fun `Object should be serialized successfully`() {
        listOf(
            StatelessObject,
            NestedStatelessObject
        ).forEach {
            serialize(it).size shouldBe 0
        }
    }

    @Test
    fun `object should be the same after serialization and deserialization`() {
        roundTripInlined(StatelessObject)
        roundTrip(StatelessObject)

        roundTripInlined(NestedStatelessObject)
        roundTrip(NestedStatelessObject)
    }

    @Test
    fun `serialization of Object with mutable properties should fail`() {
        NestedStatefulObject.myObject.myInt = 10

        listOf(
            StatefulObject,
            NestedStatefulObject
        ).forEach {
            assertThrows<SerdeError.MutablePropertiesInObject> {
                serialize(it)
            }
        }
    }

    @Serializable
    data class StatelessNullData(val value: StatelessObject?)

    @Serializable
    data class StatefulNullData(val value: StatefulObject?)

    @Test
    fun `serialization of null Object should succeed regardless of the mutability of its properties`() {
        listOf(
            StatelessNullData(null),
            StatefulNullData(null)
        ).forEach {
            assertDoesNotThrow {
                serialize(it)
            }
        }
    }
}
