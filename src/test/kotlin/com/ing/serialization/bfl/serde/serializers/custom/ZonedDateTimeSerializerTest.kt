package com.ing.serialization.bfl.serde.serializers.custom

import com.ing.serialization.bfl.deserialize
import com.ing.serialization.bfl.serialize
import com.ing.serialization.bfl.serializeX
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@ExperimentalSerializationApi
class ZonedDateTimeSerializerTest {
    @Serializable
    data class Data(val date: @Contextual ZonedDateTime)

    @Test
    fun `serialize 3rd party class`() {
        val data = Data(ZonedDateTime.now())
        println(serializeX(data).second)
    }

    @Test
    fun `serialize and deserialize 3rd party class`() {
        val data = Data(ZonedDateTime.now())
        val bytes = serialize(data)

        val deserialized: Data = deserialize(bytes)
        data shouldBe deserialized
    }
}
