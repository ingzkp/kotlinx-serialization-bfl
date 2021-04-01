package com.ing.serialization.bfl.api.reified

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

inline fun <reified T : Any> serialize(data: T, serializersModule: SerializersModule = EmptySerializersModule): ByteArray {
    val serializer = serializersModule.serializer<T>()

    return ByteArrayOutputStream().use { output ->
        DataOutputStream(output).use { stream ->
            BinaryFixedLengthOutputEncoder(stream, serializersModule).encodeSerializableValue(serializer, data)
        }
        output.toByteArray()
    }
}

inline fun <reified T : Any> debugSerialize(data: T, serializersModule: SerializersModule = EmptySerializersModule): Pair<ByteArray, Layout> {
    val serializer = serializersModule.serializer<T>()

    return ByteArrayOutputStream().use { output ->
        val layout = DataOutputStream(output).use { stream ->
            val bfl = BinaryFixedLengthOutputEncoder(stream, serializersModule)
            bfl.encodeSerializableValue(serializer, data)
            bfl.layout
        }
        Pair(output.toByteArray(), layout)
    }
}

inline fun <reified T : Any> deserialize(data: ByteArray, serializersModule: SerializersModule = EmptySerializersModule): T {
    val deserializer = serializersModule.serializer<T>()

    return ByteArrayInputStream(data).use { input ->
        DataInputStream(input).use { stream ->
            val bfl = BinaryFixedLengthInputDecoder(stream, serializersModule)
            bfl.decodeSerializableValue(deserializer)
        }
    }
}
