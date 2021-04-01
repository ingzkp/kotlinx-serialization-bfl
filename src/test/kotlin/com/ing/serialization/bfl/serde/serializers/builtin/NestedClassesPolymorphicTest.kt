package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.element.ElementFactory
import com.ing.serialization.bfl.serde.generateRSAPubKey
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.security.PublicKey

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")

class NestedClassesPolymorphicTest {
    @Serializable
    data class Some(val pk: PublicKey)

    @Serializable
    data class Data(val some: Some)

    @Test
    fun `serialize polymorphic type within nested compound type`() {
        val data = Data(Some(generateRSAPubKey()))

        var mask = listOf(
            Pair("some.pk.serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("some.pk.length", 4),
            Pair("some.nested.value", 294)
        )

        checkedSerializeInlined(data, mask)

        mask = listOf(
            Pair("some.pk.serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("some.pk.length", 4),
            Pair("some.nested.value", 294)
        )

        checkedSerialize(data, mask)
    }

    @Test
    fun `serialize and deserialize polymorphic type within nested compound type`() {
        val data = Data(Some(generateRSAPubKey()))

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `serialization has fixed length`() {
        val data1 = Data(Some(generateRSAPubKey()))
        val data2 = Data(Some(generateRSAPubKey()))

        sameSizeInlined(data1, data2)
        sameSize(data1, data2)
    }
}
