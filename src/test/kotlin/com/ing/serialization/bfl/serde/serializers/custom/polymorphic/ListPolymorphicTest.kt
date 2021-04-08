package com.ing.serialization.bfl.serde.serializers.custom.polymorphic

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.serialize
import com.ing.serialization.bfl.serde.SerdeError
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
import org.junit.jupiter.api.assertThrows
import java.security.PublicKey

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")

class ListPolymorphicTest {
    @Serializable
    data class Data(@FixedLength([2]) val nested: List<PublicKey>)

    @Test
    fun `List of polymorphic type should be serialized successfully`() {
        val mask = listOf(
            Pair("nested.length", 4),
            Pair("nested[0].serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("nested[0].length", 4),
            Pair("nested[0].value", PublicKeyBaseSurrogate.ENCODED_SIZE),
            Pair("nested[1].serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("nested[1].length", 4),
            Pair("nested[1].value", PublicKeyBaseSurrogate.ENCODED_SIZE)
        )

        val data = Data(listOf(generateRSAPubKey()))
        checkedSerializeInlined(data, mask, PolySerializers)
        checkedSerialize(data, mask, PolySerializers)
    }

    @Test
    fun `List of polymorphic type should be the same after serialization and deserialization`() {
        val data = Data(listOf(generateRSAPubKey()))

        roundTripInlined(data, PolySerializers)
        roundTrip(data, PolySerializers)
    }

    @Test
    fun `different Lists of polymorphic type should have same size after serialization`() {
        val empty = Data(listOf())
        val data1 = Data(listOf(generateRSAPubKey()))
        val data2 = Data(listOf(generateRSAPubKey()))

        sameSizeInlined(empty, data1, PolySerializers)
        sameSize(empty, data1, PolySerializers)
        sameSizeInlined(data2, data1, PolySerializers)
        sameSize(data2, data1, PolySerializers)
    }

    @Test
    fun `too long List of compound type should throw CollectionTooLarge`() {
        assertThrows<SerdeError.CollectionTooLarge> {
            serialize(
                Data(
                    listOf(
                        generateRSAPubKey(),
                        generateRSAPubKey(),
                        generateRSAPubKey()
                    )
                ),
                serializersModule = PolySerializers
            )
        }
    }
}
