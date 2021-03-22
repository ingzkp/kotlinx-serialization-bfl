package com.ing.serialization.bfl.serde

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule
import java.io.DataOutput

@Suppress("TooManyFunctions")
@ExperimentalSerializationApi
class BinaryFixedLengthOutputEncoder(
    private val output: DataOutput,
    override val serializersModule: SerializersModule
) : AbstractEncoder() {
    private val structureProcessor = FixedLengthStructureProcessor(serializersModule)

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        structureProcessor.beginStructure(descriptor)
        return this
    }

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        structureProcessor.beginCollection(collectionSize)
        encodeInt(collectionSize)
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        when {
            descriptor.isCollection -> {
                val collection = structureProcessor
                    .removeNext()
                    .expect<Element.Collection>()
                repeat(collection.padding) { encodeByte(0) }
            }
            descriptor.isStructure || descriptor.isPolymorphic -> {
                structureProcessor.removeNext()
            }
            else -> TODO("Unknown structure kind `${descriptor.kind}`")
        }
    }

    override fun <T : Any?> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        with(serializer.descriptor) {
            if (isTrulyPrimitive && value == null) {
                // Primitive elements, unlike collections and structures, are not scheduled to the queue.
                // To streamline null encoding, we exceptionally schedule a primitive element on stack.
                structureProcessor.schedulePriorityElement(Element.Primitive(serialName, kind, isNullable))
            }
        }

        super.encodeSerializableValue(serializer, value)
    }

    override fun encodeBoolean(value: Boolean) = output.writeByte(if (value) 1 else 0)
    override fun encodeByte(value: Byte) = output.writeByte(value.toInt())
    override fun encodeShort(value: Short) = output.writeShort(value.toInt())
    override fun encodeInt(value: Int) = output.writeInt(value)
    override fun encodeLong(value: Long) = output.writeLong(value)
    override fun encodeFloat(value: Float) = throw IllegalStateException("Floats are not yet supported")
    override fun encodeDouble(value: Double) = throw IllegalStateException("Doubles are not yet supported")
    override fun encodeChar(value: Char) = output.writeChar(value.toInt())

    override fun encodeString(value: String) =
        structureProcessor
            .removeNext()
            .expect<Element.Strng>()
            .encode(value, this)

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = output.writeInt(index)

    override fun encodeNull() {
        encodeBoolean(false)

        when (val element = structureProcessor.removeNext()) {
            is Element.Primitive -> element.encodeNull(this)
            is Element.Strng -> element.encodeNull(this)
            is Element.Collection -> TODO()
            is Element.Structure -> {
                repeat(element.size) { encodeByte(0) }
            }
        }
    }
    override fun encodeNotNullMark() = encodeBoolean(true)
}
