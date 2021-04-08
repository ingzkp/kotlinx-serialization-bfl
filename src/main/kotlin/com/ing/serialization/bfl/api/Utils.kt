package com.ing.serialization.bfl.api

import com.ing.serialization.bfl.serde.BinaryFixedLengthInputDecoder
import com.ing.serialization.bfl.serde.BinaryFixedLengthOutputEncoder
import com.ing.serialization.bfl.serde.element.Layout
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.SerializersModule
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

fun <T : Any> genericSerialize(data: T, serializersModule: SerializersModule, serializer: KSerializer<T>): ByteArray =
    ByteArrayOutputStream().use { output ->
        DataOutputStream(output).use { stream ->
            BinaryFixedLengthOutputEncoder(stream, serializersModule).encodeSerializableValue(serializer, data)
        }
        output.toByteArray()
    }

fun <T : Any> genericDebugSerialize(data: T, serializersModule: SerializersModule, serializer: KSerializer<T>): Pair<ByteArray, Layout> =
    ByteArrayOutputStream().use { output ->
        val layout = DataOutputStream(output).use { stream ->
            val bfl = BinaryFixedLengthOutputEncoder(stream, serializersModule)
            bfl.encodeSerializableValue(serializer, data)
            bfl.layout
        }
        Pair(output.toByteArray(), layout)
    }

fun <T : Any> genericDeserialize(
    data: ByteArray,
    serializersModule: SerializersModule,
    serializer: KSerializer<T>
): T {
    return ByteArrayInputStream(data).use { input ->
        DataInputStream(input).use { stream ->
            val bfl = BinaryFixedLengthInputDecoder(stream, serializersModule)
            bfl.decodeSerializableValue(serializer)
        }
    }
}
