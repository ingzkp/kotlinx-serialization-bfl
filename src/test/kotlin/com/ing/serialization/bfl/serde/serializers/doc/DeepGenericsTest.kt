package com.ing.serialization.bfl.serde.serializers.doc

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.serialization.bfl.api.reified.deserialize
import com.ing.serialization.bfl.api.serialize
import com.ing.serialization.bfl.serde.SerdeError
import io.kotest.assertions.throwables.shouldThrow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

class DeepGenericsTest {
    @Test
    fun serializeDirectlyShouldFail() {
        shouldThrow<SerdeError.NoTopLevelSerializer> {
            val original = CustomData("Hello World!")
            serialize(original)
        }
    }

    @Test
    fun serializeStringDataWithSurrogateShouldSucceed() {
        val original = CustomData("Hello World!")
        val serializedBytes = serialize(original, CustomDataStringSerializer)
        val deserialized: CustomData<String> = deserialize(serializedBytes, CustomDataStringSerializer)
        assert(deserialized == original) { "Expected $deserialized to be $original" }
    }

    @Test
    fun serializeIntDataWithSurrogateShouldSucceed() {
        val original = CustomData(42)
        val serializedBytes = serialize(original, CustomDataIntSerializer)
        val deserialized: CustomData<Int> = deserialize(serializedBytes, CustomDataIntSerializer)
        assert(deserialized == original) { "Expected $deserialized to be $original" }
    }

    private data class CustomData<T>(
        val value: T
    )

    @Serializable
    private data class CustomDataStringSurrogate(
        @FixedLength([42])
        val value: String
    ) : Surrogate<CustomData<String>> {
        override fun toOriginal(): CustomData<String> = CustomData(value)
    }

    private object CustomDataStringSerializer : KSerializer<CustomData<String>>
    by (
        SurrogateSerializer(CustomDataStringSurrogate.serializer()) {
            CustomDataStringSurrogate(it.value)
        }
        )

    @Serializable
    private data class CustomDataIntSurrogate(
        val value: Int
    ) : Surrogate<CustomData<Int>> {
        override fun toOriginal(): CustomData<Int> = CustomData(value)
    }

    private object CustomDataIntSerializer : KSerializer<CustomData<Int>> by (
        SurrogateSerializer(CustomDataIntSurrogate.serializer()) {
            CustomDataIntSurrogate(it.value)
        }
        )
}
