package com.ing.serialization.bfl.serde.serializers.custom

import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.util.Currency
import java.util.Locale

class CurrencySerializerTest {
    @Serializable
    data class Data(val value: @Contextual Currency)

    @Test
    fun `Currency should be serialized successfully`() {
        val mask = listOf(
            Pair("length", 2),
            Pair("value", 2 * 3)
        )

        val data = Data(Currency.getInstance(Locale.CANADA))
        checkedSerializeInlined(data, mask)
        checkedSerialize(data, mask)
    }

    @Test
    fun `Currency should be the same after serialization and deserialization`() {
        val data = Data(Currency.getInstance(Locale.CANADA))
        roundTripInlined(data)
        roundTrip(data)
    }

    @Test
    fun `different Currencies should have same size after serialization`() {
        val data1 = Data(Currency.getInstance(Locale.CANADA))
        val data2 = Data(Currency.getInstance("RUB"))

        sameSizeInlined(data1, data2)
        sameSize(data1, data2)
    }
}
