package serde

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule
import prepend
import java.io.DataOutput
import java.io.DataOutputStream

@ExperimentalSerializationApi
class IndexedDataOutputEncoder(
    private val output: DataOutput,
    override val serializersModule: SerializersModule
) : AbstractEncoder() {
    private var topLevel = true
    private val elementQueue = ArrayDeque<Element>()

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val schedulable = if (topLevel) {
            topLevel = false
            val head = ElementFactory(serializersModule).parse(descriptor)
            // Place the element to the front of the queue.
            elementQueue.prepend(head)
            head
        } else {
            // TODO: add check if the struct on the stack coincides with the current descriptor.
            elementQueue.first()
        }.expect<Element.Structure>()

        // Unwind structure's inner elements to the queue.
        elementQueue.prepend(schedulable.inner.filter { it !is Element.Primitive })

        return super.beginStructure(descriptor)
    }

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        val collection = elementQueue.first().expect<Element.Collection>()
        collection.actualLength = collectionSize

        repeat(collectionSize) {
            elementQueue.prepend(collection.inner.filter { it !is Element.Primitive })
        }

        encodeInt(collectionSize)

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
        val collection = elementQueue.removeFirst().expect<Element.Collection>()

        val collectionActualLength = collection.actualLength ?: throw SerdeError.CollectionNoActualLength(collection)
        val collectionRequiredLength = collection.requiredLength

        if (collectionRequiredLength < collectionActualLength) {
            throw SerdeError.CollectionTooLarge(collection)
        }

        // Collection may require padding.

        repeat(collection.elementSize * (collectionRequiredLength - collectionActualLength)) {
            encodeByte(0)
        }
    }

    override fun encodeBoolean(value: Boolean) = output.writeByte(if (value) 1 else 0)
    override fun encodeByte(value: Byte) = output.writeByte(value.toInt())
    override fun encodeShort(value: Short) = output.writeShort(value.toInt())
    override fun encodeInt(value: Int) = output.writeInt(value)
    override fun encodeLong(value: Long) = output.writeLong(value)
    override fun encodeFloat(value: Float) = output.writeFloat(value)
    override fun encodeDouble(value: Double) = output.writeDouble(value)
    override fun encodeChar(value: Char) = output.writeChar(value.toInt())

    override fun encodeString(value: String) {
        val actualLength = value.length

        // In output.writeUTF, length of the string is stored as short.
        // We do the same for consistency.
        encodeShort(value.length.toShort())
        value.forEach { encodeChar(it) }

        val string = elementQueue.removeFirst().expect<Element.Strng>()

        val requiredLength = string.requiredLength

        if (requiredLength < actualLength)
            throw SerdeError.StringTooLarge(actualLength, string)

        repeat(2 * (requiredLength - actualLength)) { encodeByte(0) }
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = output.writeInt(index)

    override fun encodeNull() = encodeBoolean(false)
    override fun encodeNotNullMark() = encodeBoolean(true)
}