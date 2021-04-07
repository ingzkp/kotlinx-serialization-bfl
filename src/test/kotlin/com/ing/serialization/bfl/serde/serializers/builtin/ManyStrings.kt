package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.serialize
import com.ing.serialization.bfl.serde.SerdeError
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.ing.serialization.bfl.api.reified.debugSerialize as debugSerializeInlined
import com.ing.serialization.bfl.api.reified.deserialize as deserializeInlined

class ManyStrings {
    @Serializable
    data class Data(
        @FixedLength([10])
        val commonName: String?,
        @FixedLength([10])
        val organisationUnit: String?,
        @FixedLength([10])
        val organisation: String,
        @FixedLength([10])
        val locality: String,
        @FixedLength([10])
        val state: String?,
        @FixedLength([10])
        val country: String
    )

    @Test
    fun `many Strings should be serialized and deserialized successfully`() {
        val data = Data(
            null, null, "Batman", "UT", null, "US"
        )

        val serialization = debugSerializeInlined(data)
        println(serialization.second)

        val deserialization = deserializeInlined<Data>(serialization.first)

        deserialization shouldBe data
    }

    @Test
    fun `all Strings should be the same after serialization and deserialization`() {
        val data = Data(
            null, null, "Batman", "UT", null, "US"
        )

        roundTripInlined(data)
        roundTrip(data, data::class)
    }

    @Test
    fun `different Strings should have same size after serialization`() {
        val data1 = Data(
            null, null, "Batman", "UT", null, "US"
        )
        val data2 = Data(
            null, "JL", "Batman", "UT", "Gotham", "US"
        )

        sameSizeInlined(data1, data2)
        sameSize(data1, data2)
    }

    @Test
    fun `any too long String should throw StringTooLarge`() {
        assertThrows<SerdeError.StringTooLarge> {
            serialize(
                Data(
                    null,
                    "Too long organisation unit",
                    "Batman",
                    "UT",
                    null,
                    "US"
                )
            )
        }
    }
}
