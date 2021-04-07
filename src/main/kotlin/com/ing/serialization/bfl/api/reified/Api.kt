package com.ing.serialization.bfl.api.reified

import com.ing.serialization.bfl.api.genericDebugSerialize
import com.ing.serialization.bfl.api.genericSerialize
import com.ing.serialization.bfl.serde.BinaryFixedLengthInputDecoder
import com.ing.serialization.bfl.serde.element.Layout
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import java.io.ByteArrayInputStream
import java.io.DataInputStream

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

inline fun <reified T : Any> deserialize(data: ByteArray, serializersModule: SerializersModule = EmptySerializersModule): T =
    ByteArrayInputStream(data).use { input ->
        DataInputStream(input).use { stream ->
            val bfl = BinaryFixedLengthInputDecoder(stream, serializersModule)
            bfl.decodeSerializableValue(serializersModule.serializer())
        }
    }
