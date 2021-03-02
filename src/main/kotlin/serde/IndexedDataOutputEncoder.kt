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

    private val serializingState = SerializingState()

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        if (serializingState.collectionSizingStack.isEmpty()) {
            serializingState.collectionSizingStack.addLast(ElementSizingInfo.getRoot(descriptor.serialName))
        }
        processAnnotations(descriptor)
        serializingState.collectionSizingStack.last().startByte = getCurrentByteIdx()

        return super.beginStructure(descriptor)
    }

    private fun processAnnotations(descriptor: SerialDescriptor) = (descriptor.elementsCount - 1 downTo 0)
        .filter { descriptor.getElementDescriptor(it).canBeAnnotated() }
        .forEach {
            pushToSizingStack(
                "${descriptor.serialName}.${descriptor.getElementName(it)}",
                descriptor.getElementDescriptor(it),
                descriptor.getElementAnnotations(it)
            )
        }

    private fun SerialDescriptor.canBeAnnotated() = this.kind is StructureKind
            || this.kind is PolymorphicKind
            || this.kind == SerialKind.CONTEXTUAL
            || this.kind == PrimitiveKind.STRING

    private fun pushToSizingStack(propertyName: String, descriptor: SerialDescriptor, annotations: List<Annotation>) {
        val expectedElementLengths = annotations.filterIsInstance<FixedLength>().firstOrNull()?.lengths

        if (expectedElementLengths == null) {
            serializingState.collectionSizingStack.addLast(
                ElementSizingInfo(
                    getCurrentByteIdx(),
                    isPolymorphicKind = descriptor.kind is PolymorphicKind,
                    name = propertyName
                )
            )
        } else {
            var lengthIdx = 0
            var serializingDescriptor = descriptor
            var element = ElementSizingInfo(
                numberOfElements = expectedElementLengths.getOrDefault(lengthIdx),
                isPolymorphicKind = serializingDescriptor.isPolymorphicKind(),
                name = propertyName
            )
            serializingState.collectionSizingStack.addLast(element)
            if (serializingDescriptor.kind is PrimitiveKind) {
                return
            }

            serializingDescriptor = serializingDescriptor.getElementDescriptor(0)
            while (serializingDescriptor.isListLike()) {
                lengthIdx++


                element.inner = ElementSizingInfo(
                    numberOfElements = expectedElementLengths.getOrDefault(lengthIdx),
                    isPolymorphicKind = serializingDescriptor.isPolymorphicKind(),
                    name = propertyName
                )
                element = element.inner!!
                serializingDescriptor = serializingDescriptor.getElementDescriptor(0)
            }
        }
    }

    private fun SerialDescriptor.isListLike() = this.kind == StructureKind.LIST
            || this.kind == StructureKind.MAP

    private fun IntArray.getOrDefault(idx: Int) = if (idx >= this.size) -1 else this[idx]

    private fun SerialDescriptor.isPolymorphicKind() = this.kind is PolymorphicKind

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        val collectionSizingInfo = serializingState.collectionSizingStack.last()
        collectionSizingInfo.startByte = getCurrentByteIdx()
        serializingState.lastStructureSize = -1
        encodeInt(collectionSize)

        if (collectionSizingInfo.inner != null) {
            repeat(collectionSize) { serializingState.collectionSizingStack.addLast(collectionSizingInfo.inner!!.copy()) }
        }

        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        when (descriptor.kind) {
            StructureKind.LIST -> {
                val sizingInfo = serializingState.collectionSizingStack.removeLast()

                val elementsStartByteIdx = sizingInfo.startByte + 4
                val elementSize =
                    if (serializingState.lastStructureSize == -1)
                        getElementSize(descriptor, serializersModule, defaults)
                    else
                        serializingState.lastStructureSize

                serializingState.lastStructureSize = 0
                val expectedNumberOfElements = sizingInfo.numberOfElements

                check(expectedNumberOfElements != -1) { "Collection `${descriptor.serialName}` must have FixedLength annotation" }
                val writtenBytes = getCurrentByteIdx() - elementsStartByteIdx
                val expectedBytes = elementSize * expectedNumberOfElements

                val paddingBytesLength = expectedBytes - writtenBytes
                repeat(paddingBytesLength) { encodeByte(0) }

                serializingState.lastStructureSize = expectedBytes + 4
            }
            StructureKind.MAP -> {
                TODO("Implement map support")
            }
            StructureKind.CLASS -> {
                val sizingInfo = serializingState.collectionSizingStack.removeLast()
                val currentByteIdx = getCurrentByteIdx()
                serializingState.lastStructureSize = currentByteIdx - sizingInfo.startByte
            }
            else -> TODO("Unknown structure kind `${descriptor.kind}`")
        }

        super.endStructure(descriptor)
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

        val sizingInfo = serializingState.collectionSizingStack.removeLast()
        val expectedStringLength = sizingInfo.numberOfElements
        check(expectedStringLength != -1) { "Strings should have @FixedLength annotation" }

        val paddingLength = expectedStringLength - actualStringLength
        check(paddingLength >= 0) { "Serializing string doesn't fit expected size" }

        val paddingBytesLength = 2 * paddingLength
        repeat(paddingBytesLength) { encodeByte(0) }
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = output.writeInt(index)

    override fun encodeNull() = encodeBoolean(false)
    override fun encodeNotNullMark() = encodeBoolean(true)

    private fun DataOutput.getCurrentByteIdx(): Int = (this as DataOutputStream).size()

    private fun getCurrentByteIdx(): Int = (output as DataOutputStream).size()
}