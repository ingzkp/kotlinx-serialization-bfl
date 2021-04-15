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

fun <T : Any> genericSerialize(
    data: T,
    serializersModule: SerializersModule,
    serializer: KSerializer<T>,
    outerFixedLength: IntArray
): ByteArray =
    ByteArrayOutputStream().use { output ->
        DataOutputStream(output).use { stream ->
            BinaryFixedLengthOutputEncoder(stream, serializersModule, outerFixedLength)
                .encodeSerializableValue(serializer, data)
        }
        output.toByteArray()
    }

fun <T : Any> genericDebugSerialize(
    data: T,
    serializersModule: SerializersModule,
    serializer: KSerializer<T>,
    outerFixedLength: IntArray
): Pair<ByteArray, Layout> =
    ByteArrayOutputStream().use { output ->
        val layout = DataOutputStream(output).use { stream ->
            val bfl = BinaryFixedLengthOutputEncoder(stream, serializersModule, outerFixedLength)
            bfl.encodeSerializableValue(serializer, data)
            bfl.layout
        }
        Pair(output.toByteArray(), layout)
    }

fun <T : Any> genericDeserialize(
    data: ByteArray,
    serializersModule: SerializersModule,
    serializer: KSerializer<T>,
    outerFixedLength: IntArray
): T {
    return ByteArrayInputStream(data).use { input ->
        DataInputStream(input).use { stream ->
            BinaryFixedLengthInputDecoder(stream, serializersModule, outerFixedLength)
                .decodeSerializableValue(serializer)
        }
    }
}
