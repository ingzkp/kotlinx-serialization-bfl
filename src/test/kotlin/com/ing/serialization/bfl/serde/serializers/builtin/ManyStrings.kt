package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.deserialize
import com.ing.serialization.bfl.serializeX
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

@ExperimentalSerializationApi
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

        val serialization = serializeX(data)
        println(serialization.second)

        val deserialization = deserialize<Data>(serialization.first)

        deserialization shouldBe data
    }
}
