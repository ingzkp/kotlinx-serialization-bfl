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
import java.io.DataInput

@ExperimentalSerializationApi
class DataInputDecoder(
    var elementsCount: Int = 0,
    private val input: DataInput,
    override val serializersModule: SerializersModule,
    private val decodingState: DecodingState = DecodingState(),
    private val defaults: List<Any>
) : AbstractDecoder() {
    private var elementIndex = 0

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        if (descriptor.kind !is StructureKind.MAP && descriptor.kind !is StructureKind.LIST) {
            return beginClass(descriptor)
        }
        return this
    }

    private fun beginClass(descriptor: SerialDescriptor): CompositeDecoder {
        // Annotations are only accessible at the properties level.

        // When this function is called there must be a Structure on top of the stack.
        val head = decodingState.elementStack.peek().expect<Element.Structure>()

        // TODO: add check if the struct on the stack coincides with the current descriptor.

        if (head.isResolved) {
            unwindStructureToStack(descriptor)
        } else {
            // Corner case: If descriptor is polymorphic, it wall have a string attached to it.
            // if this string is split from the rest of the structure we cannot infer its size.
            if (descriptor.isPolymorphic) {
                val scheduled = Element.fromType(descriptor.serialName, descriptor)
                decodingState.elementStack.push(scheduled)
                unwindStructureToStack(descriptor)
            } else {
                (descriptor.elementsCount - 1 downTo 0).forEach { idx ->
                    val scheduled = Element.fromProperty(descriptor, idx)
                    decodingState.elementStack.push(scheduled)
                }
            }
            head.isResolved = true
        }

        return this
    }

    private fun unwindStructureToStack(descriptor: SerialDescriptor) {
        val structureMeta = decodingState.elementStack.peek().expect<Element.Structure>()

        if (structureMeta.inner.isEmpty()) {
            decodingState.elementStack.push(Element.Structure(descriptor.serialName, isResolved = false))
        }

        // Structure's inner elements need to be unwound on the stack.
        structureMeta.inner
            .filter { it !is Element.Primitive }
            .asReversed()
            .forEach { meta -> decodingState.elementStack.push(meta.copy()) }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        when (descriptor.kind) {
            is StructureKind.LIST, StructureKind.MAP -> endCollection(descriptor)
            is StructureKind.CLASS -> decodingState.elementStack.pop()
            is PolymorphicKind -> decodingState.elementStack.pop()
            else -> TODO("Unknown structure kind `${descriptor.kind}`")
        }

        super.endStructure(descriptor)
    }

    private fun endCollection(descriptor: SerialDescriptor) {
        // it's a crutch, but because of enabled sequential decoding (due to performance reasons),
        // key-value serializers don't call endStructure() on elements of list-like structures
        while (decodingState.elementStack.peek() !is Element.Collected) {
            decodingState.elementStack.pop()
        }
        val collection = decodingState.elementStack.pop().expect<Element.Collected>()

        val startByte = collection.startByte ?:throw SerdeError.CollectionNoStart(collection)
        val collectionActualLength = collection.collectionActualLength ?: throw SerdeError.CollectionNoActualLength(collection)
        val collectionRequiredLengthWrapped = collection.collectionRequiredLength ?: throw SerdeError.CollectedNoRequiredLength(collection)

        val collectionRequiredLength = when (collectionRequiredLengthWrapped) {
            is Length.Actual -> return
            is Length.Fixed -> collectionRequiredLengthWrapped.value
        }

        if (collectionRequiredLength == collectionActualLength) {
            // No padding is required.
            return
        } else if (collectionRequiredLength < collectionActualLength) {
            throw SerdeError.CollectedTooLarge(collection)
        }
        // Collected is to be padded.

        val elementsStartByteIdx = startByte + 4

        // writtenBytes is the amount of bytes corresponding serialization of inner elements.
        val writtenBytes = decodingState.byteIndex - elementsStartByteIdx

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
        val string = (0 until actualLength).map { decodeChar() }.joinToString("")

        val sizingInfo = decodingState.elementStack.pop().expect<Element.Collected>()

        val requiredLength = sizingInfo.collectionRequiredLength?.let {
            when (it) {
                is Length.Actual -> return string
                is Length.Fixed -> it.value
            }
        } ?: throw SerdeError.CollectedNoRequiredLength(sizingInfo)

        if (actualLength > requiredLength)
            throw SerdeError.StringSizingMismatch(actualLength.toInt(), requiredLength)

        val paddingLength = requiredLength - actualLength

        val paddingBytesLength = 2 * paddingLength
        input.skipBytes(paddingBytesLength)

        return string
    }

    override fun decodeBoolean(): Boolean = input.readBoolean().also { decodingState.byteIndex++ }
    override fun decodeByte() = input.readByte().also { decodingState.byteIndex++ }
    override fun decodeShort() = input.readShort().also { decodingState.byteIndex += 2 }
    override fun decodeInt() = input.readInt().also { decodingState.byteIndex += 4 }
    override fun decodeLong() = input.readLong().also { decodingState.byteIndex += 8 }
    override fun decodeFloat() = input.readFloat().also { decodingState.byteIndex += 4 }
    override fun decodeDouble() = input.readDouble().also { decodingState.byteIndex += 8 }
    override fun decodeChar() = input.readChar().also { decodingState.byteIndex += 2 }
    override fun decodeEnum(enumDescriptor: SerialDescriptor) = input.readInt().also { decodingState.byteIndex += 4 }

    override fun decodeSequentially(): Boolean = true

    override fun decodeCollectionSize(descriptor: SerialDescriptor) =
        decodeInt().also { elementsCount = it }.also { beginCollection(descriptor) }

    private fun beginCollection(descriptor: SerialDescriptor): CompositeDecoder {
        // Unwind sizing meta information for this collection to the stack.
        val collectionMeta = decodingState.elementStack.peek().expect<Element.Collected>()

        collectionMeta.startByte = decodingState.byteIndex
        collectionMeta.collectionActualLength = elementsCount

        repeat(elementsCount) {
            collectionMeta.inner
                .filter { it !is Element.Primitive }
                .asReversed()
                .forEach { meta -> decodingState.elementStack.push(meta.copy()) }
        }

        return this
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor) =
        if (elementIndex == elementsCount) CompositeDecoder.DECODE_DONE else elementIndex++

    override fun decodeNotNullMark(): Boolean = decodeBoolean()

    private fun <T> ArrayDeque<T>.push(value: T) = this.addFirst(value)
    private fun <T> ArrayDeque<T>.pop(): T = this.removeFirst()
    private fun <T> ArrayDeque<T>.peek(): T = this.first()

    data class DecodingState(var byteIndex: Int = 0, val elementStack: ArrayDeque<Element> = ArrayDeque(listOf(Element.Structure("ROOT", isResolved = false))))
}