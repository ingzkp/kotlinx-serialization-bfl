package com.ing.serialization.bfl.serde.serializers.doc

import com.ing.serialization.bfl.api.reified.deserialize
import com.ing.serialization.bfl.api.serialize
import com.ing.serialization.bfl.serde.SerdeError
import com.ing.serialization.bfl.serializers.CurrencySerializer
import io.kotest.assertions.throwables.shouldThrow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import org.junit.jupiter.api.Test
import java.util.Currency
import java.util.Locale

class SerializableGenericsTest {
    @Test
    fun serializeDirectlyShouldFail() {
        shouldThrow<SerdeError.NoTopLevelSerializer> {
            val original = CustomData("Hello World!")
            serialize(original)
        }
    }

    @Test
    fun serializeStringDataWithSurrogateFailsWithInsufficientLenght() {
        shouldThrow<SerdeError.InsufficientLengthData> {
            val original = CustomData("Hello World!")
            val strategy = CustomData.serializer(String.serializer())
            val serializedBytes = serialize(original, strategy)
            val deserialized: CustomData<String> = deserialize(serializedBytes, strategy)
            assert(deserialized == original) { "Expected $deserialized to be $original" }
        }
    }

    @Test
    fun serializeIntDataWithSurrogateShouldSucceed() {
        val original = CustomData(42)
        val strategy = CustomData.serializer(Int.serializer())
        val serializedBytes = serialize(original, strategy)
        val deserialized: CustomData<Int> = deserialize(serializedBytes, strategy)
        assert(deserialized == original) { "Expected $deserialized to be $original" }
    }

    @Test
    fun serializeCurrencyDataWithSurrogateShouldSucceed() {
        val original = CustomData(Currency.getInstance(Locale.ITALY))
        val strategy = CustomData.serializer(CurrencySerializer)
        val serializedBytes = serialize(original, strategy)
        val deserialized: CustomData<Currency> = deserialize(serializedBytes, strategy)
        assert(deserialized == original) { "Expected $deserialized to be $original" }
    }

    @Serializable
    private data class CustomData<T>(
        val value: T
    )
}
