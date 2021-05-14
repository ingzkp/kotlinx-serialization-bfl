package com.ing.serialization.bfl.serde.serializers.doc

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.serialization.bfl.api.reified.deserialize
import com.ing.serialization.bfl.api.reified.serialize
import com.ing.serialization.bfl.serde.SerdeError
import io.kotest.assertions.throwables.shouldThrow
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test

class ImplementationTest {
    @Test
    fun serializeDirectlyShouldFail() {
        shouldThrow<SerdeError.NoTopLevelSerializer> {
            val original = CustomData("Hello World!")
            serialize(original)
        }
    }

    @Test
    fun serializeWithSurrogateShouldSucceed() {
        val original = CustomData("Hello World!")
        val serializedBytes = serialize(original, serializersModule = customDataSerializationModule)
        val deserialized: CustomData = deserialize(serializedBytes, serializersModule = customDataSerializationModule)
        assert(deserialized == original) { "Expected $deserialized to be $original" }
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

object CustomDataSerializer :
    SurrogateSerializer<CustomData, CustomDataSurrogate>(CustomDataSurrogate.serializer(), { CustomDataSurrogate(it.value) })

val customDataSerializationModule = SerializersModule {
    contextual(CustomDataSerializer)
}
