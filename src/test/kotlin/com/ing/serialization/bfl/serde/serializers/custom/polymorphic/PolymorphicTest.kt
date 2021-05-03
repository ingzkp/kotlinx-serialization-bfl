package com.ing.serialization.bfl.serde.serializers.custom.polymorphic

import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.generateRSAPubKey
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import org.junit.jupiter.api.Test

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
class PolymorphicTest {
    @Test
    fun `Polymorphic type itself should be serialized successfully`() {
        val data = generateRSAPubKey()

        println(data.encoded.joinToString(separator = ","))

        val mask = listOf(
            Pair("serialName", PublicKeyBaseSurrogate.SERIAL_NAME_LENGTH),
            Pair("length", 4),
            Pair("value", PublicKeyBaseSurrogate.ENCODED_SIZE)
        )
        checkedSerializeInlined(data, mask, PolySerializers)
    }

    @Test
    fun `Polymorphic type should be the same after serialization and deserialization`() {
        val data = generateRSAPubKey()

        roundTripInlined(data, PolySerializers)
        roundTrip(data, PolySerializers)
    }

    @Test
    fun `different Polymorphic data objects should have same size after serialization`() {
        val data1 = generateRSAPubKey()
        val data2 = generateRSAPubKey()

        roundTripInlined(data1, PolySerializers)
        roundTrip(data1, PolySerializers)

        sameSizeInlined(data1, data2, PolySerializers)
        sameSize(data1, data2, PolySerializers)
    }
}
