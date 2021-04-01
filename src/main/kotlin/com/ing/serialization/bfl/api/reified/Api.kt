package com.ing.serialization.bfl

import com.ing.serialization.bfl.serde.BinaryFixedLengthInputDecoder
import com.ing.serialization.bfl.serde.BinaryFixedLengthOutputEncoder
import com.ing.serialization.bfl.serde.element.Layout
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

inline fun <reified T : Any> inlinedSerialize(data: T, serializersModule: SerializersModule = EmptySerializersModule): ByteArray {
    val serializer = serializersModule.serializer<T>()
    return ByteArrayOutputStream().use { output ->
        DataOutputStream(output).use { stream ->
            BinaryFixedLengthOutputEncoder(stream, serializersModule).encodeSerializableValue(serializer, data)
        }
        output.toByteArray()
    }
}

fun <T : Any> serialize(data: T, serializersModule: SerializersModule = EmptySerializersModule): ByteArray {
    val serializer = serializersModule.serializer(data::class.java)

    val output = ByteArrayOutputStream()
    val stream = DataOutputStream(output)
    BinaryFixedLengthOutputEncoder(stream, serializersModule)
        .encodeSerializableValue(serializer, data)
    return output.toByteArray()
}

inline fun <reified T : Any> serializeX(data: T, serializersModule: SerializersModule = EmptySerializersModule): Pair<ByteArray, Layout> =
    ByteArrayOutputStream().use { output ->
        val layout = DataOutputStream(output).use { stream ->
            val bfl = BinaryFixedLengthOutputEncoder(stream, serializersModule)
            bfl.encodeSerializableValue(serializersModule.serializer(), data)
            bfl.layout
        }
        Pair(output.toByteArray(), layout)
    }

inline fun <reified T : Any> deserialize(data: ByteArray, serializersModule: SerializersModule = EmptySerializersModule): T =
    ByteArrayInputStream(data).use { input ->
        DataInputStream(input).use { stream ->
            val bfl = BinaryFixedLengthInputDecoder(stream, serializersModule)
            bfl.decodeSerializableValue(serializersModule.serializer())
        }
    }

fun <T> ArrayDeque<T>.prepend(value: T) {
    addFirst(value)
}

fun <T> ArrayDeque<T>.prepend(list: List<T>) {
    addAll(0, list)
}
