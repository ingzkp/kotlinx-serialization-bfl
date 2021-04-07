package com.ing.serialization.bfl.serializers

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

val BFLSerializers = SerializersModule {
    // Contextual types.
    contextual(BigDecimalSerializer)
    contextual(CurrencySerializer)
    contextual(DateSerializer)
    contextual(ZonedDateTimeSerializer)
}
