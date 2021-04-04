package com.ing.serialization.bfl.api.reified

import com.ing.serialization.bfl.api.genericDebugSerialize
import com.ing.serialization.bfl.api.genericSerialize
import com.ing.serialization.bfl.serde.BinaryFixedLengthInputDecoder
import com.ing.serialization.bfl.serde.element.Layout
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import java.io.ByteArrayInputStream
import java.io.DataInputStream

inline fun <reified T : Any> serialize(data: T, serializersModule: SerializersModule = EmptySerializersModule): ByteArray {
    val serializer = serializersModule.serializer<T>()
    return genericSerialize(data, serializersModule, serializer)
}

inline fun <reified T : Any> debugSerialize(data: T, serializersModule: SerializersModule = EmptySerializersModule): Pair<ByteArray, Layout> {
    val serializer = serializersModule.serializer<T>()

    return genericDebugSerialize(data, serializersModule, serializer)
}

inline fun <reified T : Any> deserialize(data: ByteArray, serializersModule: SerializersModule = EmptySerializersModule): T =
    ByteArrayInputStream(data).use { input ->
        DataInputStream(input).use { stream ->
            val bfl = BinaryFixedLengthInputDecoder(stream, serializersModule)
            bfl.decodeSerializableValue(serializersModule.serializer())
        }
    }
