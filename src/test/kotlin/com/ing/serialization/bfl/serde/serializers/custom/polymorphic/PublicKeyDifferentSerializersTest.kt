package com.ing.serialization.bfl.serde.serializers.custom.polymorphic

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serde.generateDSAPubKey
import com.ing.serialization.bfl.serde.generateRSAPubKey
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.security.PublicKey

class PublicKeyDifferentSerializersTest {
    @Serializable
    data class Data(@FixedLength([2]) val list: List<PublicKey>)

    @Test
    fun `serialize different variants of a polymorphic type`() {
        val data = Data(listOf(generateRSAPubKey(), generateDSAPubKey()))

        roundTrip(data, data::class, PolySerializers)
        roundTripInlined(data, PolySerializers)
    }
}
