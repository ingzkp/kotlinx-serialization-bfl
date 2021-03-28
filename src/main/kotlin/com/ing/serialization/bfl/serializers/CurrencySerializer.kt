package com.ing.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.Currency

@ExperimentalSerializationApi
object CurrencySerializer : KSerializer<Currency> {
    private val strategy = CurrencySurrogate.serializer()
    override val descriptor: SerialDescriptor = strategy.descriptor

    override fun deserialize(decoder: Decoder): Currency {
        val surrogate = decoder.decodeSerializableValue(strategy)
        return Currency.getInstance(surrogate.code)
    }

    override fun serialize(encoder: Encoder, value: Currency) {
        encoder.encodeSerializableValue(strategy, CurrencySurrogate(value.currencyCode))
    }
}

@ExperimentalSerializationApi
@Serializable
/**
 * ISO 4217: Currency codes are composed of a country's two-character Internet country code
 * plus a third character denoting the currency unit.
 */
data class CurrencySurrogate(@FixedLength([3]) val code: String)
