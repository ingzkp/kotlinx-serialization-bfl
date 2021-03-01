package serde

import annotations.FixedLength
import getElementSize
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import serializers.RSAPublicKeySerializer
import sun.security.rsa.RSAPublicKeyImpl
import java.io.DataOutput
import java.io.DataOutputStream
import java.security.PublicKey

@ExperimentalSerializationApi
class IndexedDataOutputEncoder(private val output: DataOutput, private val defaults: List<Any>) : AbstractEncoder() {

    private val serializingState = SerializingState()

    override val serializersModule: SerializersModule = SerializersModule {
        polymorphic(PublicKey::class) {
            subclass(RSAPublicKeyImpl::class, RSAPublicKeySerializer)
        }
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        if (serializingState.collectionSizingStack.isEmpty()) {
            serializingState.collectionSizingStack.addLast(ElementSizingInfo.getRoot(descriptor.serialName))
        }

        descriptor.elementNames.toList()
            .indices
            .reversed()
            .forEach {
                if (descriptor.getElementDescriptor(it).kind !is PrimitiveKind || descriptor.getElementDescriptor(it).kind == PrimitiveKind.STRING) {
                    pushToSizingStack(
                        "${descriptor.serialName}.${descriptor.elementNames.toList()[it]}",
                        descriptor.getElementDescriptor(it),
                        descriptor.getElementAnnotations(it)
                    )
                }
            }

        serializingState.collectionSizingStack.last().startByte = getCurrentByteIdx()

        return super.beginStructure(descriptor)
    }

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
            pushRecursivelyWithContainer(
                expectedElementLengths = expectedElementLengths,
                propertyName = propertyName,
                descriptor = descriptor
            )
        }
    }

    private fun pushRecursivelyWithContainer(
        container: ElementSizingInfo? = null,
        numberOfElements: Int = 1,
        expectedElementLengths: IntArray,
        lengthIdx: Int = 0,
        propertyName: String,
        descriptor: SerialDescriptor) {
        if (descriptor.kind is PrimitiveKind && descriptor.kind != PrimitiveKind.STRING) {
            return
        }
        for (i in 0 until numberOfElements) {
            val sizingInfo = ElementSizingInfo(
                -1,
                numberOfElements = if (lengthIdx == expectedElementLengths.size) -1 else expectedElementLengths[lengthIdx],
                container = container,
                isPolymorphicKind = descriptor.kind is PolymorphicKind,
                name = propertyName
            )
            serializingState.collectionSizingStack.addLast(sizingInfo)
            if (descriptor.kind != PrimitiveKind.STRING) {
                pushRecursivelyWithContainer(sizingInfo, sizingInfo.numberOfElements, expectedElementLengths, lengthIdx + 1, propertyName, descriptor.getElementDescriptor(0))
            }
        }
    }

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        val container = serializingState.collectionSizingStack.last().container
        if (container != null && !container.isRemovedRedundant) {
            removeRedundantSizingInfo(container, collectionSize)
        }
        serializingState.collectionSizingStack.last().startByte = getCurrentByteIdx()
        serializingState.lastStructureSize = -1
        encodeInt(collectionSize)
        return this
    }

    private fun removeRedundantSizingInfo(container: ElementSizingInfo, actualCollectionSize: Int) {
        val expectedNumberOfElements = container.numberOfElements
        repeat(expectedNumberOfElements - actualCollectionSize) { serializingState.collectionSizingStack.removeLast() }
        container.isRemovedRedundant = true
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        when (descriptor.kind) {
            StructureKind.LIST -> {
                val sizingInfo = serializingState.collectionSizingStack.removeLast()
                val container = sizingInfo.container

                if (container != null && container.startByte == -1) {
                    container.startByte = sizingInfo.startByte - 4
                }
                val elementsStartByteIdx = sizingInfo.startByte + 4
                val elementSize =
                    if (serializingState.lastStructureSize == -1)
                        getElementSize(descriptor, defaults)
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

                val container = sizingInfo.container
                if (container != null && container.numberOfElements != -1) {
                    container.startByte = sizingInfo.startByte - 4
                }
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