package com.ing.serialization.bfl

import com.ing.serialization.bfl.serde.BinaryFixedLengthInputDecoder
import com.ing.serialization.bfl.serde.BinaryFixedLengthOutputEncoder
import com.ing.serialization.bfl.serde.element.Layout
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

@ExperimentalSerializationApi
inline fun <reified T : Any> serialize(
    data: T,
    serializersModule: SerializersModule = EmptySerializersModule
): ByteArray {
    return serialize(data, serializer(), serializersModule)
}

@ExperimentalSerializationApi
inline fun <reified T : Any> serializeX(
    data: T,
    serializersModule: SerializersModule = EmptySerializersModule
): Pair<ByteArray, Layout> {
    return serializeX(data, serializer(), serializersModule)
}

@ExperimentalSerializationApi
inline fun <reified T : Any> deserialize(
    data: ByteArray,
    serializersModule: SerializersModule = EmptySerializersModule
): T {
    return deserialize(data, serializer(), serializersModule)
}

@ExperimentalSerializationApi
fun <T> serialize(
    data: T,
    serializer: SerializationStrategy<T>,
    serializersModule: SerializersModule = EmptySerializersModule
): ByteArray {
    return ByteArrayOutputStream().use { output ->
        DataOutputStream(output).use { stream ->
            BinaryFixedLengthOutputEncoder(stream, serializersModule)
                .encodeSerializableValue(serializer, data)
        }
        output.toByteArray()
    }
}

@ExperimentalSerializationApi
fun <T : Any> serializeX(
    data: T,
    serializer: SerializationStrategy<T>,
    serializersModule: SerializersModule = EmptySerializersModule
): Pair<ByteArray, Layout> {
    return ByteArrayOutputStream().use { output ->
        val layout = DataOutputStream(output).use { stream ->
            val bfl = BinaryFixedLengthOutputEncoder(stream, serializersModule)
            bfl.encodeSerializableValue(serializer, data)
            bfl.layout
        }
        Pair(output.toByteArray(), layout)
    }
}

@ExperimentalSerializationApi
fun <T : Any> deserialize(
    data: ByteArray,
    deserializer: DeserializationStrategy<T>,
    serializersModule: SerializersModule = EmptySerializersModule
): T {
    return ByteArrayInputStream(data).use { input ->
        DataInputStream(input).use { stream ->
            val bfl = BinaryFixedLengthInputDecoder(stream, serializersModule)
            bfl.decodeSerializableValue(deserializer)
        }
    }
}

fun <T> ArrayDeque<T>.prepend(value: T) {
    addFirst(value)
}

fun <T> ArrayDeque<T>.prepend(list: List<T>) {
    addAll(0, list)
}
