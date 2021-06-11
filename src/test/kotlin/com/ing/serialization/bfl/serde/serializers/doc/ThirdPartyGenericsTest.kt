package com.ing.serialization.bfl.serde.serializers.doc

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.serialization.bfl.serde.SerdeError
import io.kotest.assertions.throwables.shouldThrow
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test
import java.util.Currency
import java.util.Locale
import com.ing.serialization.bfl.api.reified.deserialize as deserializeInlined
import com.ing.serialization.bfl.api.reified.serialize as serializeInlined

class ThirdPartyGenericsTest {
    @Test
    fun serializeDirectlyShouldFail() {
        shouldThrow<SerdeError.NoTopLevelSerializer> {
            val original = CustomData("Hello World!")
            serializeInlined(original)
        }
    }

    @Test
    fun serializeStringDataWithSurrogateShouldSucceed() {
        val original = CustomData("Hello World!")
        val serializedBytes = serializeInlined(original, CustomDataStringSerializer)
        val deserialized: CustomData<String> = deserializeInlined(serializedBytes, CustomDataStringSerializer)
        assert(deserialized == original) { "Expected $deserialized to be $original" }
    }

    @Test
    fun serializeCurrencyDataWithSurrogateShouldSucceed() {
        val original = CustomData(Currency.getInstance(Locale.JAPAN))
        val serializedBytes = serializeInlined(original, CustomDataCurrencySerializer)
        val deserialized: CustomData<Currency> = deserializeInlined(serializedBytes, CustomDataCurrencySerializer)
        assert(deserialized == original) { "Expected $deserialized to be $original" }
    }

    @Test
    fun serializeGenericStringDataWithSurrogateShouldSucceed() {
        val original = CustomData("Hello World!")
        val serializersModule = SerializersModule {
            contextual(CustomDataSerializer(String.serializer()))
        }
        val serializedBytes = serializeInlined(original, serializersModule = serializersModule, outerFixedLength = intArrayOf(20))
        val deserialized: CustomData<String> = deserializeInlined(serializedBytes, serializersModule = serializersModule, outerFixedLength = intArrayOf(20))
        assert(deserialized == original) { "Expected $deserialized to be $original" }
    }

    data class CustomData<T>(
        val value: T
    )

    @Serializable
    private data class CustomDataStringSurrogate(
        @FixedLength([42])
        val value: String
    ) : Surrogate<CustomData<String>> {
        override fun toOriginal(): CustomData<String> = CustomData(value)
    }

    private object CustomDataStringSerializer :
        SurrogateSerializer<CustomData<String>, CustomDataStringSurrogate>(
            CustomDataStringSurrogate.serializer(), { CustomDataStringSurrogate(it.value) }
        )

    @Serializable
    private data class CustomDataCurrencySurrogate(
        val value: @Contextual Currency
    ) : Surrogate<CustomData<Currency>> {
        override fun toOriginal(): CustomData<Currency> = CustomData(value)
    }

    private object CustomDataCurrencySerializer : SurrogateSerializer<CustomData<Currency>, CustomDataCurrencySurrogate>(
        CustomDataCurrencySurrogate.serializer(), { CustomDataCurrencySurrogate(it.value) }
    )

    @Serializable
    private data class CustomDataSurrogate<T : Any>(
        val value: @Contextual T
    ) : Surrogate<CustomData<T>> {
        override fun toOriginal(): CustomData<T> = CustomData(value)
    }

    private class CustomDataSerializer<T : Any>(valueSerializer: KSerializer<T>) :
        SurrogateSerializer<CustomData<T>, CustomDataSurrogate<T>>(
            CustomDataSurrogate.serializer(valueSerializer),
            { CustomDataSurrogate(it.value) }
        )
}
