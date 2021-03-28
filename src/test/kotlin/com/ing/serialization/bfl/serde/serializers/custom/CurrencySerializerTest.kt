package com.ing.serialization.bfl.serde.serializers.custom

import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.sameSize
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.util.Currency
import java.util.Locale

@ExperimentalSerializationApi
class CurrencySerializerTest {
    @Serializable
    data class Data(val value: @Contextual Currency)

    @Test
    fun `serialize Currency`() {
        val mask = listOf(
            Pair("length", 2),
            Pair("value", 2 * 3)
        )

        val data = Data(Currency.getInstance(Locale.CANADA))
        checkedSerialize(data, mask)
    }

    @Test
    fun `serialize and deserialize Currency`() {
        val data = Data(Currency.getInstance(Locale.CANADA))
        roundTrip(data)
    }

    @Test
    fun `same size Currency`() {
        val data1 = Data(Currency.getInstance(Locale.CANADA))
        val data2 = Data(Currency.getInstance("RUB"))

        sameSize(data1, data2)
    }
}
