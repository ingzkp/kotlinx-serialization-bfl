package com.ing.serialization.bfl.serde

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule
import java.io.DataInput

@Suppress("TooManyFunctions")
@ExperimentalSerializationApi
class BinaryFixedLengthInputDecoder(
    private val input: DataInput,
    override val serializersModule: SerializersModule
) : AbstractDecoder() {
    private var elementIndex = 0
    private val structureProcessor = FixedLengthStructureProcessor(serializersModule)

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        if (!descriptor.isCollection) {
            structureProcessor.beginStructure(descriptor)
        }
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        when {
            descriptor.isCollection -> endCollection()
            descriptor.isStructure || descriptor.isPolymorphic -> structureProcessor.removeNext()
            else -> TODO("Unknown structure kind `${descriptor.kind}`")
        }
    }

    private fun endCollection() {
        // it's a crutch, but because of enabled sequential decoding (due to performance reasons),
        // key-value (aka Pair instances) serializers don't call endStructure() on elements of list-like structures
        while (structureProcessor.peekNext() !is Element.Collection) {
            structureProcessor.removeNext()
        }

        val collection = structureProcessor
            .removeNext()
            .expect<Element.Collection>()

        // Collection might have been padded.
        input.skipBytes(collection.padding)
    }

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        with(deserializer.descriptor) {
            if (isTrulyPrimitive && isNullable) {
                // Primitive elements, unlike collections and structures, are not scheduled to the queue.
                // If it will be read that the some following data represents a null,
                // we need to know what element this is to skip an appropriate number of bytes in `decodeNull`.
                // Thus an extra element is scheduled to the front of the queue.
                structureProcessor.schedulePriorityElement(Element.Primitive(serialName, kind, isNullable))
            }
        }

        return super.decodeSerializableValue(deserializer)
    }

    override fun decodeString() = structureProcessor
        .removeNext()
        .expect<Element.Strng>()
        .decode(this)

    override fun decodeBoolean(): Boolean = input.readBoolean()
    override fun decodeByte() = input.readByte()
    override fun decodeShort() = input.readShort()
    override fun decodeInt() = input.readInt()
    override fun decodeLong() = input.readLong()
    override fun decodeFloat() = input.readFloat()
    override fun decodeDouble() = input.readDouble()
    override fun decodeChar() = input.readChar()
    override fun decodeEnum(enumDescriptor: SerialDescriptor) = input.readInt()

    override fun decodeSequentially(): Boolean = true

    override fun decodeCollectionSize(descriptor: SerialDescriptor) =
        decodeInt().also { structureProcessor.beginCollection(it) }

    override fun decodeElementIndex(descriptor: SerialDescriptor) =
        if (elementIndex == structureProcessor.peekNext().expect<Element.Collection>().actualLength) {
            CompositeDecoder.DECODE_DONE
        } else {
            elementIndex++
        }

    override fun decodeNotNullMark(): Boolean {
        val isNotNull = decodeBoolean()
        if (isNotNull) {
            if (structureProcessor.peekNext() is Element.Primitive) {
                structureProcessor.removeNext()
            }
        }

        return isNotNull
    }

    override fun decodeNull(): Nothing? {
        val element = structureProcessor.removeNext()
        skipBytes(element.size)

        return null
    }

    fun skipBytes(n: Int) = input.skipBytes(n)
}
