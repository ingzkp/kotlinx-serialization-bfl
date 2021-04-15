package com.ing.serialization.bfl.api.reified

import com.ing.serialization.bfl.api.genericDebugSerialize
import com.ing.serialization.bfl.api.genericDeserialize
import com.ing.serialization.bfl.api.genericSerialize
import com.ing.serialization.bfl.serde.SerdeError
import com.ing.serialization.bfl.serde.element.Layout
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer

inline fun <reified T : Any> serialize(
    data: T,
    strategy: KSerializer<T>? = null,
    serializersModule: SerializersModule = EmptySerializersModule,
    outerFixedLength: IntArray = IntArray(0)
): ByteArray {
    val serializer = getSerializer(strategy, serializersModule)
    return genericSerialize(data, serializersModule, serializer, outerFixedLength)
}

inline fun <reified T : Any> debugSerialize(
    data: T,
    strategy: KSerializer<T>? = null,
    serializersModule: SerializersModule = EmptySerializersModule,
    outerFixedLength: IntArray = IntArray(0)
): Pair<ByteArray, Layout> {
    val serializer = getSerializer(strategy, serializersModule)
    return genericDebugSerialize(data, serializersModule, serializer, outerFixedLength)
}

inline fun <reified T : Any> deserialize(
    data: ByteArray,
    strategy: KSerializer<T>? = null,
    serializersModule: SerializersModule = EmptySerializersModule,
    outerFixedLength: IntArray = IntArray(0)
): T {
    val serializer = getSerializer(strategy, serializersModule)
    return genericDeserialize(data, serializersModule, serializer, outerFixedLength)
}

inline fun <reified T : Any> getSerializer(
    strategy: KSerializer<T>?,
    serializersModule: SerializersModule
) = try {
    strategy ?: serializersModule.serializer()
} catch (e: SerializationException) {
    throw SerdeError.NoTopLevelSerializer(T::class, e)
}
