package com.ing.serialization.bfl.api

import com.ing.serialization.bfl.serde.SerdeError
import com.ing.serialization.bfl.serde.element.Layout
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializerOrNull
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
private fun <T : Any> SerializersModule.getSerializerFor(type: KClass<out T>): KSerializer<T> =
    this.serializerOrNull(type.java) as? KSerializer<T>
        ?: throw SerdeError.NoTopLevelSerializer(type)

fun <T : Any> serialize(
    data: T,
    strategy: KSerializer<T>? = null,
    serializersModule: SerializersModule = EmptySerializersModule
): ByteArray {
    val serializer = strategy ?: serializersModule.getSerializerFor(data::class)
    return genericSerialize(data, serializersModule, serializer)
}

fun <T : Any> debugSerialize(
    data: T,
    strategy: KSerializer<T>? = null,
    serializersModule: SerializersModule = EmptySerializersModule
): Pair<ByteArray, Layout> {
    val serializer = strategy ?: serializersModule.getSerializerFor(data::class)
    return genericDebugSerialize(data, serializersModule, serializer)
}

fun <T : Any> deserialize(
    data: ByteArray,
    klass: KClass<out T>,
    strategy: KSerializer<out T>? = null,
    serializersModule: SerializersModule = EmptySerializersModule
): T {
    val deserializer = strategy ?: serializersModule.getSerializerFor(klass)
    return genericDeserialize(data, serializersModule, deserializer)
}
