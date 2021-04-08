package com.ing.serialization.bfl.serde.serializers.doc

import com.ing.serialization.bfl.api.serialize
import com.ing.serialization.bfl.serde.SerdeError
import com.ing.serialization.bfl.serializers.BFLSerializers
import com.ing.serialization.bfl.serializers.CurrencySerializer
import io.kotest.assertions.throwables.shouldThrow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.EmptySerializersModule
import org.junit.jupiter.api.Test
import java.util.Currency
import java.util.Locale
import com.ing.serialization.bfl.api.reified.deserialize as deserializeInlined
import com.ing.serialization.bfl.api.reified.serialize as serializeInlined

class ProjectGenericsTest {
    @Test
    fun serializeDirectlyShouldFail() {
        shouldThrow<SerdeError.NoTopLevelSerializer> {
            val original = CustomData("Hello World!")
            serialize(original)
        }
    }

    @Test
    fun serializeStringDataWithSurrogateFailsWithInsufficientLength() {
        shouldThrow<SerdeError.InsufficientLengthData> {
            val original = CustomData("Hello World!")
            val strategy = CustomData.serializer(String.serializer())
            val serializedBytes = serializeInlined(original)
            val deserialized: CustomData<String> = deserializeInlined(serializedBytes)
            assert(deserialized == original) { "Expected $deserialized to be $original" }
        }
    }

    @Test
    fun serializeIntDataWithSurrogateShouldSucceed() {
        val original = CustomData(42)
        val strategy = CustomData.serializer(Int.serializer())
        val serializedBytes = serializeInlined(original)
        val deserialized: CustomData<Int> = deserializeInlined(serializedBytes)
        assert(deserialized == original) { "Expected $deserialized to be $original" }
    }

    @Test
    fun serializeCurrencyDataWithSurrogateShouldSucceed() {
        val original = CustomData(Currency.getInstance(Locale.ITALY))
        val strategy = CustomData.serializer(CurrencySerializer)
        val serializedBytes = serializeInlined(original, strategy = null, serializersModule = BFLSerializers)
        val deserialized: CustomData<Currency> = deserializeInlined(serializedBytes, strategy = strategy, serializersModule = EmptySerializersModule)
        assert(deserialized == original) { "Expected $deserialized to be $original" }
    }

    @Serializable
    private data class CustomData<T>(
        val value: T
    )
}
