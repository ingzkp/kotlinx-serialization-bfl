package com.ing.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import java.util.Currency

object CurrencySerializer : KSerializer<Currency> by (SurrogateSerializer(CurrencySurrogate.serializer()) { CurrencySurrogate(it.currencyCode) })

@Serializable
/**
 * ISO 4217: Currency codes are composed of a country's two-character Internet country code
 * plus a third character denoting the currency unit.
 */
data class CurrencySurrogate(@FixedLength([CURRENCY_SIZE]) val code: String) : Surrogate<Currency> {
    override fun toOriginal(): Currency = Currency.getInstance(code)

    companion object {
        const val CURRENCY_SIZE = 3
        fun from(currency: Currency): CurrencySurrogate = CurrencySurrogate(currency.currencyCode)
    }
}
