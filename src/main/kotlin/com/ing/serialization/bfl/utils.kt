package com.ing.serialization.bfl

import com.ing.serialization.bfl.serde.BinaryFixedLengthInputDecoder
import com.ing.serialization.bfl.serde.BinaryFixedLengthOutputEncoder
import com.ing.serialization.bfl.serde.element.Layout
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

@ExperimentalSerializationApi
inline fun <reified T : Any> serialize(data: T, serializersModule: SerializersModule = EmptySerializersModule): ByteArray {
    val output = ByteArrayOutputStream()
    val stream = DataOutputStream(output)
    BinaryFixedLengthOutputEncoder(stream, serializersModule).encodeSerializableValue(serializer(), data)
    return output.toByteArray()
}

@ExperimentalSerializationApi
inline fun <reified T : Any> serializeX(data: T, serializersModule: SerializersModule = EmptySerializersModule): Pair<ByteArray, Layout> {
    val output = ByteArrayOutputStream()
    val stream = DataOutputStream(output)
    val bfl = BinaryFixedLengthOutputEncoder(stream, serializersModule)
    bfl.encodeSerializableValue(serializer(), data)
    return Pair(output.toByteArray(), bfl.layout)
}

@ExperimentalSerializationApi
inline fun <reified T : Any> deserialize(data: ByteArray, serializersModule: SerializersModule = EmptySerializersModule): T {
    val input = ByteArrayInputStream(data)
    val stream = DataInputStream(input)
    val bfl = BinaryFixedLengthInputDecoder(stream, serializersModule)
    return bfl.decodeSerializableValue(serializer())
}

fun <T> ArrayDeque<T>.prepend(value: T) {
    addFirst(value)
}

fun <T> ArrayDeque<T>.prepend(list: List<T>) {
    addAll(0, list)
}
