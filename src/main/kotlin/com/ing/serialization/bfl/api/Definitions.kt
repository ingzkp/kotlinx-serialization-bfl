package com.ing.serialization.bfl.api

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

open class BaseSerializer<T, S : Surrogate<T>>(
    private val strategy: KSerializer<S>,
    private val toSurrogate: (T) -> S
) : KSerializer<T> {
    override val descriptor: SerialDescriptor = strategy.descriptor

    override fun deserialize(decoder: Decoder): T {
        val surrogate = decoder.decodeSerializableValue(strategy)
        return surrogate.toOriginal()
    }

    override fun serialize(encoder: Encoder, value: T) {
        val surrogate = toSurrogate(value)
        encoder.encodeSerializableValue(strategy, surrogate)
    }
}

interface Surrogate<T> {
    fun toOriginal(): T
}
