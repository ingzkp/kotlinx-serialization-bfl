package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.annotations.FixedLength
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
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
    fun `serialize and deserialize many strings`() {
        val data = Data(
            null, null, "Batman", "UT", null, "US"
        )

        val serialization = debugSerializeInlined(data)
        println(serialization.second)

        val deserialization = deserializeInlined<Data>(serialization.first)

        deserialization shouldBe data
    }
}
