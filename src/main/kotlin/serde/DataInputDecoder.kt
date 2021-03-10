package serde

import getElementSize
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule
import peek
import pop
import push
import java.io.DataInput

@ExperimentalSerializationApi
class DataInputDecoder(
    private val input: DataInput,
    override val serializersModule: SerializersModule,
    private val defaults: List<Any>
) : AbstractDecoder() {
    private var elementIndex = 0
    private var elementsCount: Int = 0
    private var byteIndex: Int = 0
    private val elementStack: ArrayDeque<Element>
        = ArrayDeque(listOf(Element.Structure("ROOT", inner= listOf(), isResolved = false)))

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        if (!descriptor.isCollection) {
            return beginClass(descriptor)
        }

        return this
    }

    private fun beginClass(descriptor: SerialDescriptor): CompositeDecoder {
        // Annotations are only accessible at the properties level.

        // When this function is called there must be a Structure on top of the stack.
        val head = elementStack.peek().expect<Element.Structure>()

        // TODO: add check if the struct on the stack coincides with the current descriptor.

        if (head.isResolved) {
            unwindStructureToStack(descriptor)
        } else {
            // Corner case: If descriptor is polymorphic, it wall have a string attached to it.
            // if this string is split from the rest of the structure we cannot infer its size.
            if (descriptor.isPolymorphic) {
                val scheduled = Element.fromType(descriptor.serialName, descriptor)
                elementStack.push(scheduled)
                unwindStructureToStack(descriptor)
            } else {
                (descriptor.elementsCount - 1 downTo 0).forEach { idx ->
                    val scheduled = Element.fromProperty(descriptor, idx)
                    elementStack.push(scheduled)
                }
            }
            head.isResolved = true
        }

        return this
    }

    private fun unwindStructureToStack(descriptor: SerialDescriptor) {
        val structureMeta = elementStack.peek().expect<Element.Structure>()

        if (structureMeta.inner.isEmpty()) {
            elementStack.push(Element.Structure(descriptor.serialName, inner = listOf(), isResolved = false))
        }

        // Structure's inner elements need to be unwound on the stack.
        structureMeta.inner
            .filter { it !is Element.Primitive }
            .asReversed()
            .forEach { meta -> elementStack.push(meta.copy()) }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        when (descriptor.kind) {
            is StructureKind.LIST, StructureKind.MAP -> endCollection(descriptor)
            is StructureKind.CLASS -> elementStack.pop()
            is PolymorphicKind -> elementStack.pop()
            else -> TODO("Unknown structure kind `${descriptor.kind}`")
        }

        super.endStructure(descriptor)
    }

    private fun endCollection(descriptor: SerialDescriptor) {
        // it's a crutch, but because of enabled sequential decoding (due to performance reasons),
        // key-value serializers don't call endStructure() on elements of list-like structures
        while (elementStack.peek() !is Element.Collection) {
            elementStack.pop()
        }
        val collection = elementStack.pop().expect<Element.Collection>()

        val startByte = collection.startByte ?:throw SerdeError.CollectionNoStart(collection)
        val collectionActualLength = collection.actualLength ?: throw SerdeError.CollectionNoActualLength(collection)
        val collectionRequiredLength = collection.requiredLength

        if (collectionRequiredLength == collectionActualLength) {
            // No padding is required.
            return
        } else if (collectionActualLength > collectionRequiredLength) {
            throw SerdeError.CollectionTooLarge(collection)
        }
        // Collection is to be padded.

        val elementsStartByteIdx = startByte + 4

        // writtenBytes is the amount of bytes corresponding serialization of inner elements.
        val writtenBytes = byteIndex - elementsStartByteIdx

        val elementSize = if (collectionActualLength != 0) {
            writtenBytes / collectionActualLength
        } else {
            if (collection.inner.size != descriptor.elementsCount)
                throw SerdeError.CollectionSizingMismatch(collection, descriptor.elementsCount)

            collection.inner.zip(descriptor.elementDescriptors).sumBy { (childSizingInfo, childDescriptor) ->
                getElementSize(childDescriptor, childSizingInfo, serializersModule, defaults)
            }
        }

        input.skipBytes(elementSize * (collectionRequiredLength - collectionActualLength))
    }

    override fun decodeString(): String {
        // In output.writeUTF, length of the string is stored as short.
        // We do the same for consistency.
        val actualLength = decodeShort()
        val value = (0 until actualLength).map { decodeChar() }.joinToString("")

        val string = elementStack.pop().expect<Element.Strng>()

        val requiredLength = string.requiredLength

        if (actualLength > requiredLength) {
            throw SerdeError.StringTooLarge(actualLength.toInt(), string)
        }

        val paddingLength = requiredLength - actualLength

        val paddingBytesLength = 2 * paddingLength
        input.skipBytes(paddingBytesLength)

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
        decodeInt().also { elementsCount = it }.also { beginCollection(descriptor) }

    private fun beginCollection(descriptor: SerialDescriptor): CompositeDecoder {
        // Unwind sizing meta information for this collection to the stack.
        val collectionMeta = elementStack.peek().expect<Element.Collection>()

        collectionMeta.startByte = byteIndex
        collectionMeta.actualLength = elementsCount

        repeat(elementsCount) {
            collectionMeta.inner
                .filter { it !is Element.Primitive }
                .asReversed()
                .forEach { meta -> elementStack.push(meta.copy()) }
        }

        return this
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor) =
        if (elementIndex == elementsCount) CompositeDecoder.DECODE_DONE else elementIndex++

    override fun decodeNotNullMark(): Boolean = decodeBoolean()
}