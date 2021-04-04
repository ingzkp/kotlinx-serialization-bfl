package com.ing.serialization.bfl.api

import com.ing.serialization.bfl.serde.BinaryFixedLengthOutputEncoder
import com.ing.serialization.bfl.serde.element.Layout
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.SerializersModule
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

fun <T : Any> genericSerialize(data: T, serializersModule: SerializersModule, serializer: KSerializer<T>) =
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
