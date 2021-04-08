package com.ing.serialization.bfl.api.reified

import com.ing.serialization.bfl.api.genericDebugSerialize
import com.ing.serialization.bfl.api.genericDeserialize
import com.ing.serialization.bfl.api.genericSerialize
import com.ing.serialization.bfl.serde.element.Layout
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer

inline fun <reified T : Any> serialize(
    data: T,
    strategy: KSerializer<T>? = null,
    serializersModule: SerializersModule = EmptySerializersModule
): ByteArray {
    val serializer = strategy ?: serializersModule.serializer()
    return genericSerialize(data, serializersModule, serializer)
}

inline fun <reified T : Any> debugSerialize(
    data: T,
    strategy: KSerializer<T>? = null,
    serializersModule: SerializersModule = EmptySerializersModule
): Pair<ByteArray, Layout> {
    val serializer = strategy ?: serializersModule.serializer()
    return genericDebugSerialize(data, serializersModule, serializer)
}

inline fun <reified T : Any> deserialize(
    data: ByteArray,
    strategy: KSerializer<T>? = null,
    serializersModule: SerializersModule = EmptySerializersModule
): T {
    val serializer = strategy ?: serializersModule.serializer()
    return genericDeserialize(data, serializersModule, serializer)
}
