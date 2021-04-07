package com.ing.serialization.bfl.api

import com.ing.serialization.bfl.serde.BinaryFixedLengthInputDecoder
import com.ing.serialization.bfl.serde.SerdeError
import com.ing.serialization.bfl.serde.element.Layout
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializerOrNull
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import kotlin.reflect.KClass

fun <T : Any> serialize(
    data: T,
    strategy: KSerializer<T>? = null,
    serializersModule: SerializersModule = EmptySerializersModule
): ByteArray {
    val serializer = strategy
        ?: @Suppress("UNCHECKED_CAST")
        serializersModule.serializerOrNull(data::class.java) as? KSerializer<T>
        ?: throw SerdeError.NoTopLevelSerializer(data::class)

    return genericSerialize(data, serializersModule, serializer)
}

fun <T : Any> debugSerialize(
    data: T,
    strategy: KSerializer<T>? = null,
    serializersModule: SerializersModule = EmptySerializersModule
): Pair<ByteArray, Layout> {
    val serializer = strategy
        ?: @Suppress("UNCHECKED_CAST")
        serializersModule.serializerOrNull(data::class.java) as? KSerializer<T>
        ?: throw SerdeError.NoTopLevelSerializer(data::class)

    return genericDebugSerialize(data, serializersModule, serializer)
}

fun <T : Any> deserialize(
    data: ByteArray,
    klass: KClass<out T>,
    strategy: KSerializer<out T>? = null,
    serializersModule: SerializersModule = EmptySerializersModule
): T {
    val deserializer = strategy ?: serializersModule.serializerOrNull(klass.java)
        ?: throw SerdeError.NoTopLevelSerializer(klass)

    return ByteArrayInputStream(data).use { input ->
        DataInputStream(input).use { stream ->
            val bfl = BinaryFixedLengthInputDecoder(stream, serializersModule)
            @Suppress("UNCHECKED_CAST")
            bfl.decodeSerializableValue(deserializer) as? T
                ?: throw SerdeError.CannotDeserializeAs(data, klass)
        }
    }
}
