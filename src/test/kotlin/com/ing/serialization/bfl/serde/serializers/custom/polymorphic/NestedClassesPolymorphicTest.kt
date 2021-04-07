package com.ing.serialization.bfl.serde.serializers.custom.polymorphic

import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.element.ElementFactory
import com.ing.serialization.bfl.serde.generateDSAPubKey
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
    fun `Polymorphic type within nested compound type should be serialized successfully`() {
        val data = Data(Some(generateRSAPubKey()))

        val mask = listOf(
            Pair("some.pk.serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("some.pk.length", 4),
            Pair("some.nested.value", PublicKeyBaseSurrogate.ENCODED_SIZE)
        )

        checkedSerializeInlined(data, mask, PolySerializers)
        checkedSerialize(data, mask, PolySerializers)
    }

    @Test
    fun `Polymorphic type within nested compound type should be the same after serialization and deserialization`() {
        val data = Data(Some(generateRSAPubKey()))

        roundTripInlined(data, PolySerializers)
        roundTrip(data, data::class, PolySerializers)
    }

    @Test
    fun `different data objects of Polymorphic type within nested compound type should have same size after serialization`() {
        val data1 = Data(Some(generateRSAPubKey()))
        val data2 = Data(Some(generateDSAPubKey()))

        sameSizeInlined(data1, data2, PolySerializers)
        sameSize(data1, data2, PolySerializers)
    }
}
