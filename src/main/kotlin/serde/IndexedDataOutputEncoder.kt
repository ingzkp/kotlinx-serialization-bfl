package serde

import annotations.FixedLength
import getElementSize
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule
import java.io.DataOutput
import java.io.DataOutputStream

@ExperimentalSerializationApi
class IndexedDataOutputEncoder(
    private val output: DataOutput,
    override val serializersModule: SerializersModule,
    private vararg val defaults: Any
) : AbstractEncoder() {

    private var lastStructureSize: Int? = null
    private val elementMetaStack: ArrayDeque<ElementSerializingMeta> = ArrayDeque()

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        pushStructureMetaToStack(descriptor)
        processStructureFields(descriptor)
        elementMetaStack.peek().startByte = getCurrentByteIdx()

        return super.beginStructure(descriptor)
    }

    private fun pushStructureMetaToStack(descriptor: SerialDescriptor) =
        elementMetaStack.push(ElementSerializingMeta(startByte = getCurrentByteIdx(), name = descriptor.serialName))

    private fun processStructureFields(descriptor: SerialDescriptor) =
        (descriptor.elementsCount - 1 downTo 0)
            .filter { descriptor.getElementDescriptor(it).canBeAnnotated() }
            .forEach {
                val annotations = descriptor.getElementAnnotations(it)
                val expectedElementLengths = annotations
                    .filterIsInstance<FixedLength>()
                    .firstOrNull()?.lengths
                    ?: error("Element ${descriptor.serialName}.${descriptor.getElementName(it)} must have FixedLength annotations")

                scheduleCollectionMetaToStack(
                    "${descriptor.serialName}.${descriptor.getElementName(it)}",
                    descriptor.getElementDescriptor(it),
                    expectedElementLengths
                )
            }

    // todo bad naming. why "is StructureKind"?
    private fun SerialDescriptor.canBeAnnotated() = kind is StructureKind
            || kind is PolymorphicKind
            || kind is SerialKind.CONTEXTUAL
            || kind is PrimitiveKind.STRING


    private fun scheduleCollectionMetaToStack(
        name: String,
        elementDescriptor: SerialDescriptor,
        lengths: IntArray
    ) {
        var lengthIdx = 0
        var descriptor = elementDescriptor
        var element = ElementSerializingMeta(numberOfElements = lengths.getOrNull(lengthIdx), name = name)
        elementMetaStack.push(element)
        if (descriptor.kind is PrimitiveKind) {
            return
        }
        // todo: are we confident there will be only one element descriptor here by the time we reach this code?
        //   i don't see any prior checks, because map is also a collection.
        //   this loop is a recursion imitation
        descriptor = descriptor.getElementDescriptor(0)
        while (descriptor.isCollection()) {
            lengthIdx++

            element.inner =
                ElementSerializingMeta(numberOfElements = lengths.getOrNull(lengthIdx), name = name)
            element = element.inner!!

            descriptor = descriptor.getElementDescriptor(0)
        }
    }

    private fun SerialDescriptor.isCollection() = this.kind is StructureKind.LIST || this.kind is StructureKind.MAP

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        val collectionMeta = elementMetaStack.peek()
        collectionMeta.startByte = getCurrentByteIdx()
        collectionMeta.inner?.let { inner -> repeat(collectionSize) {
            elementMetaStack.push(inner.copy()) }
        }

        lastStructureSize = null
        encodeInt(collectionSize)

        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        when (descriptor.kind) {
            StructureKind.LIST -> endList(descriptor)
            StructureKind.MAP -> TODO("Implement map support")
            StructureKind.CLASS -> {
                val sizingInfo = elementMetaStack.pop()
                val currentByteIdx = getCurrentByteIdx()
                check(sizingInfo.startByte != null) { "Class `${sizingInfo.name}` has no start byte index" }
                lastStructureSize = currentByteIdx - sizingInfo.startByte!!
            }
            else -> TODO("Unknown structure kind `${descriptor.kind}`")
        }

        super.endStructure(descriptor)
    }

    private fun endList(descriptor: SerialDescriptor) {
        val sizingInfo = elementMetaStack.pop()

        check(sizingInfo.startByte != null) { "List `${sizingInfo.name}` has no start byte index" }
        val elementsStartByteIdx = sizingInfo.startByte!! + 4
        val elementSize = lastStructureSize ?: getElementSize(descriptor, serializersModule, defaults)

        lastStructureSize = null
        val expectedNumberOfElements = sizingInfo.numberOfElements

        check(expectedNumberOfElements != null) { "List `${descriptor.serialName}` must have FixedLength annotation" }
        val writtenBytes = getCurrentByteIdx() - elementsStartByteIdx
        val expectedBytes = elementSize * expectedNumberOfElements

        val paddingBytesLength = expectedBytes - writtenBytes
        repeat(paddingBytesLength) { encodeByte(0) }

        lastStructureSize = expectedBytes + 4
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

        val sizingInfo = elementMetaStack.pop()
        val expectedStringLength = sizingInfo.numberOfElements
        check(expectedStringLength != null) { "Strings should have @FixedLength annotation" }

        val paddingLength = expectedStringLength - actualStringLength
        check(paddingLength >= 0) { "Serializing string doesn't fit expected size" }

        val paddingBytesLength = 2 * paddingLength
        repeat(paddingBytesLength) { encodeByte(0) }
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = output.writeInt(index)

    override fun encodeNull() = encodeBoolean(false)
    override fun encodeNotNullMark() = encodeBoolean(true)

    private fun getCurrentByteIdx(): Int = (output as DataOutputStream).size()

    private fun <T> ArrayDeque<T>.push(value: T) = this.addLast(value)
    private fun <T> ArrayDeque<T>.pop(): T = this.removeLast()
    private fun <T> ArrayDeque<T>.peek(): T = this.last()
}