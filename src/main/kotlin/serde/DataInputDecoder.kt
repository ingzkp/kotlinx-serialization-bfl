package serde

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule
import prepend
import java.io.DataInput

@ExperimentalSerializationApi
class DataInputDecoder(
    private val input: DataInput,
    override val serializersModule: SerializersModule
) : AbstractDecoder() {
    private lateinit var structure: Element

    private var elementIndex = 0
    private var byteIndex: Int = 0

    private var topLevel = true
    private val elementQueue = ArrayDeque<Element>()

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        if (descriptor.isCollection) {
            return this
        }

        return beginClass(descriptor)
    }

    private fun beginClass(descriptor: SerialDescriptor): CompositeDecoder {
        val schedulable = if (topLevel) {
            topLevel = false
            structure = ElementFactory(serializersModule).parse(descriptor)
            // Place the element to the front of the queue.
            elementQueue.prepend(structure)
            structure
        } else {
            // TODO: add check if the struct on the stack coincides with the current descriptor.
            elementQueue.first()
        }.expect<Element.Structure>()

        // Unwind structure's inner elements to the queue.
        elementQueue.prepend(schedulable.inner.filter { it !is Element.Primitive })

        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        when (descriptor.kind) {
            is StructureKind.LIST, StructureKind.MAP -> endCollection()
            is StructureKind.CLASS -> elementQueue.removeFirst()
            is PolymorphicKind -> elementQueue.removeFirst()
            else -> TODO("Unknown structure kind `${descriptor.kind}`")
        }

        super.endStructure(descriptor)
    }

    private fun endCollection() {
        // it's a crutch, but because of enabled sequential decoding (due to performance reasons),
        // key-value serializers don't call endStructure() on elements of list-like structures
        while (elementQueue.first() !is Element.Collection) {
            elementQueue.removeFirst()
        }

        val collection = elementQueue.removeFirst().expect<Element.Collection>()

        val collectionActualLength = collection.actualLength ?: throw SerdeError.CollectionNoActualLength(collection)
        val collectionRequiredLength = collection.requiredLength

        if (collectionRequiredLength < collectionActualLength) {
            throw SerdeError.CollectionTooLarge(collection)
        }

        // Collection might have been padded.

        input.skipBytes(collection.elementSize * (collectionRequiredLength - collectionActualLength))
    }

    override fun decodeString(): String {
        // In output.writeUTF, length of the string is stored as short.
        // We do the same for consistency.
        val actualLength = decodeShort()
        val value = (0 until actualLength).map { decodeChar() }.joinToString("")

        val string = elementQueue.removeFirst().expect<Element.Strng>()

        val requiredLength = string.requiredLength

        if (requiredLength < actualLength) {
            throw SerdeError.StringTooLarge(actualLength.toInt(), string)
        }

        input.skipBytes(2 * (requiredLength - actualLength))

        return value
    }

    override fun decodeBoolean(): Boolean = input.readBoolean().also { byteIndex++ }
    override fun decodeByte() = input.readByte().also { byteIndex++ }
    override fun decodeShort() = input.readShort().also { byteIndex += 2 }
    override fun decodeInt() = input.readInt().also { byteIndex += 4 }
    override fun decodeLong() = input.readLong().also { byteIndex += 8 }
    override fun decodeFloat() = input.readFloat().also { byteIndex += 4 }
    override fun decodeDouble() = input.readDouble().also { byteIndex += 8 }
    override fun decodeChar() = input.readChar().also { byteIndex += 2 }
    override fun decodeEnum(enumDescriptor: SerialDescriptor) = input.readInt().also { byteIndex += 4 }

    override fun decodeSequentially(): Boolean = true

    override fun decodeCollectionSize(descriptor: SerialDescriptor) =
        decodeInt().also { beginCollection(it) }

    private fun beginCollection(collectionSize: Int): CompositeDecoder {
        val collection = elementQueue.first().expect<Element.Collection>()
        collection.actualLength = collectionSize

        repeat(collectionSize) {
            elementQueue.prepend(collection.inner.filter { it !is Element.Primitive })
        }

        return this
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor) =
        if (elementIndex == elementQueue.first().expect<Element.Collection>().actualLength) {
            CompositeDecoder.DECODE_DONE
        } else {
            elementIndex++
        }

    override fun decodeNotNullMark(): Boolean = decodeBoolean()
}