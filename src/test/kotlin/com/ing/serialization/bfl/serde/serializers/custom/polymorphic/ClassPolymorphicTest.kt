package com.ing.serialization.bfl.serde.serializers.custom.polymorphic

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
class ClassPolymorphicTest {
    @Serializable
    data class Data(val pk: PublicKey)

    @Test
    fun `Polymorphic type within structure should be serialized successfully`() {
        val mask = listOf(
            Pair("pk.serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("pk.value", 4 + PublicKeyBaseSurrogate.ENCODED_SIZE)
        )

        val data = Data(generateRSAPubKey())
        checkedSerializeInlined(data, mask, PolySerializers)
        checkedSerialize(data, mask, PolySerializers)
    }

    @Test
    fun `Polymorphic type within structure should be the same after serialization and deserialization`() {
        val data = Data(generateRSAPubKey())

        roundTripInlined(data, PolySerializers)
        roundTrip(data, PolySerializers)
    }

    @Test
    fun `different Polymorphic data objects should have same size after serialization`() {
        val data1 = Data(generateRSAPubKey())
        val data2 = Data(generateRSAPubKey())

        sameSizeInlined(data2, data1, PolySerializers)
        sameSize(data2, data1, PolySerializers)
    }
}
