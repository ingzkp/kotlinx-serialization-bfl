package com.ing.serialization.bfl.serializers

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

val BFLSerializers = SerializersModule {
    // Contextual types.
    contextual(BigDecimalSerializer)
    contextual(DoubleSerializer)
    contextual(FloatSerializer)
    contextual(CurrencySerializer)
    contextual(DateSerializer)
    contextual(InstantSerializer)
    contextual(UUIDSerializer)
    contextual(DurationSerializer)
    contextual(X500PrincipalSerializer)
}
