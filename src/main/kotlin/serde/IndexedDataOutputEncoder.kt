package serde

import getElementSize
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule
import java.io.DataOutput
import java.io.DataOutputStream

@ExperimentalSerializationApi
class IndexedDataOutputEncoder(
    private val output: DataOutput,
    override val serializersModule: SerializersModule,
    private val defaults: List<Any>
) : AbstractEncoder() {

    private val elementStack: ArrayDeque<Element> = ArrayDeque()

    init {
        elementStack.push(Element.Structure("ROOT", isResolved = false))
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        // Annotations are only accessible at the properties level.

        // When this function is called there must be a Structure on top of the stack.
        val head = elementStack.peek()
        check(head is Element.Structure) { "Structure may begin only when the head of the stack is Element.Structure" }

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

        return super.beginStructure(descriptor)
    }

    private fun unwindStructureToStack(descriptor: SerialDescriptor) {
        val structureMeta = elementStack.peek()
        check(structureMeta is Element.Structure) { "Stack element must describe a structure type" }

        if (structureMeta.inner.isEmpty()) {
            elementStack.push(Element.Structure(descriptor.serialName, isResolved = false))
        }

        // Structure's inner elements need to be unwound on the stack.
        structureMeta.inner
            .filter { it !is Element.Primitive }
            .asReversed()
            .forEach { meta -> elementStack.push(meta.copy()) }
    }

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        // Unwind sizing meta information for this collection to the stack.
        val collectionMeta = elementStack.peek()
        check(collectionMeta is Element.Collected) { "Stack element must describe a collection type" }

        collectionMeta.startByte = getCurrentByteIdx()
        collectionMeta.collectionActualLength = collectionSize

        repeat(collectionSize) {
            collectionMeta.inner
                .filter { it !is Element.Primitive }
                .asReversed()
                .forEach { meta -> elementStack.push(meta.copy()) }
        }

        encodeInt(collectionSize)

        return this
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
        val sizingInfo = elementStack.pop()
        check(sizingInfo is Element.Collected) { "Stack element must describe a compound (List or Map) type" }
        val name = sizingInfo.name
        val (startByte, collectionActualLength, collectionRequiredLengthWrapped, innerSizingInfo) = sizingInfo.sizingInfo

        check(startByte != null) { "Structure `$name` has no start byte index" }
        check(collectionActualLength != null) { "Structure `$name` does not specify its actual size" }
        check(collectionRequiredLengthWrapped != null) { "Structure `$name` does not specify its required size" }

        val collectionRequiredLength = when (collectionRequiredLengthWrapped) {
            is Length.Actual -> return
            is Length.Fixed -> collectionRequiredLengthWrapped.value
        }

        if (collectionRequiredLength == collectionActualLength) {
            // No padding is required.
            return
        }
        // Collected is to be padded.

        val elementsStartByteIdx = startByte + 4

        // writtenBytes is the amount of bytes corresponding serialization of inner elements.
        val writtenBytes = getCurrentByteIdx() - elementsStartByteIdx

        val elementSize = if (collectionActualLength != 0) {
            writtenBytes / collectionActualLength
        } else {
            check(innerSizingInfo.size == descriptor.elementsCount) { "Sizing info does not coincide with descriptors"}

            innerSizingInfo.zip(descriptor.elementDescriptors).sumBy { (childSizingInfo, childDescriptor) ->
                getElementSize(childDescriptor, childSizingInfo, serializersModule, defaults)
            }
        }

        repeat(elementSize * (collectionRequiredLength - collectionActualLength)) {
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
        val actualStringLength = value.length

        // In output.writeUTF, length of the string is stored as short.
        // We do the same for consistency.
        encodeShort(value.length.toShort())
        value.forEach { encodeChar(it) }

        val sizingInfo = elementStack.pop()
        check(sizingInfo is Element.Collected) { "Stack element must describe a compound (String) type" }

        val stringRequiredLength = sizingInfo.collectionRequiredLength?.let {
            when (it) {
                is Length.Actual -> return
                is Length.Fixed -> it.value
            }
        } ?: error( "Strings should have fixed length" )

        val paddingLength = stringRequiredLength - actualStringLength
        check(paddingLength >= 0) { "Serializing string doesn't fit expected size" }

        val paddingBytesLength = 2 * paddingLength
        repeat(paddingBytesLength) { encodeByte(0) }
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = output.writeInt(index)

    override fun encodeNull() = encodeBoolean(false)
    override fun encodeNotNullMark() = encodeBoolean(true)

    private fun getCurrentByteIdx(): Int = (output as DataOutputStream).size()

    private fun <T> ArrayDeque<T>.push(value: T) = this.addFirst(value)
    private fun <T> ArrayDeque<T>.pop(): T = this.removeFirst()
    private fun <T> ArrayDeque<T>.peek(): T = this.first()
}