package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.serialization.bfl.api.reified.deserialize
import com.ing.serialization.bfl.api.reified.serialize
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test

class ImplementationTest {
    @Test
    fun serializeDirectlyShouldFail() {
        shouldThrow<SerializationException> {
            val original = CustomData("Hello World!")
            serialize(original)
        }
    }

    @Test
    fun serializeWithSurrogateShouldSucceed() {
        val original = CustomData("Hello World!")
        val serializedBytes = serialize(original, customDataSerializationModule)
        val deserialized: CustomData = deserialize(serializedBytes, customDataSerializationModule)
        deserialized shouldBe original
    }
}

data class CustomData(val value: String)

@Serializable
data class CustomDataSurrogate(
    @FixedLength([42])
    val value: String
) : Surrogate<CustomData> {
    override fun toOriginal(): CustomData = CustomData(value)
}

object CustomDataSerializer : KSerializer<CustomData>
by (
    SurrogateSerializer(CustomDataSurrogate.serializer()) {
        CustomDataSurrogate(it.value)
    }
    )

val customDataSerializationModule = SerializersModule {
    contextual(CustomDataSerializer)
}
