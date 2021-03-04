package serde

import annotations.DFLength
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

    // private var lastStructureSize: Int? = null
    private val sizingInfoStack: ArrayDeque<SizingInfo> = ArrayDeque()

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        pushStructureMetaToStack(descriptor)
        processStructureFields(descriptor)

        // This holds by stack construction.
        (sizingInfoStack.peek() as SizingInfo.Compound).startByte = getCurrentByteIdx()

        return super.beginStructure(descriptor)
    }

    private fun pushStructureMetaToStack(descriptor: SerialDescriptor) =
        sizingInfoStack.push(SizingInfo.Compound(
            ElementSizingInfoImpl(startByte = getCurrentByteIdx(), name = descriptor.serialName)
        ))

    private fun processStructureFields(descriptor: SerialDescriptor) =
        (descriptor.elementsCount - 1 downTo 0)
            // .filter { descriptor.getElementDescriptor(it).mustBeAnnotated() }
            .filter { descriptor.getElementDescriptor(it).isUnbounded }
            .forEach {
                val name = "${descriptor.serialName}.${descriptor.getElementName(it)}"
                val annotations = descriptor.getElementAnnotations(it)

                // DFLength must always be present for all unbounded types.
                val dfLength = annotations
                    .filterIsInstance<DFLength>()
                    .firstOrNull()?.lengths
                    ?: error("Element $name must have DFLength annotation")

                scheduleCollectionMetaToStack(
                    descriptor.serialName,
                    descriptor.getElementDescriptor(it),
                    dfLength.toMutableList()
                )
            }

    private val SerialDescriptor.isCollection: Boolean
        get() = kind is StructureKind.LIST || kind is StructureKind.MAP

    private val SerialDescriptor.isUnbounded: Boolean
        get() = isCollection || kind is PrimitiveKind.STRING

    // // todo get back to using this after the inner question is addressed
    // private fun SerialDescriptor.mustBeAnnotated() =
    //     kind is StructureKind.LIST
    //         || kind is StructureKind.MAP
    //         || kind is PrimitiveKind.STRING
    //         // todo; explain better why the last two are required.
    //         || kind is PolymorphicKind
    //         || kind is SerialKind.CONTEXTUAL

    private fun scheduleCollectionMetaToStack(
        parentName: String,
        descriptor: SerialDescriptor,
        lengths: MutableList<Int>
    ) {
        fun child(parentName: String, descriptor: SerialDescriptor, lengths: MutableList<Int>): SizingInfo {
            if (!descriptor.isUnbounded) {
                return SizingInfo.Bounded
            }

            val name = "$parentName.${descriptor.serialName}"
            val requiredSize = lengths.removeFirstOrNull() ?: error("Insufficient sizing info for $name")

            val children = descriptor.elementDescriptors.map {
                child(name, it, lengths)
            }

            return SizingInfo.Compound(
                ElementSizingInfoImpl(collectionRequiredSize = requiredSize, inner = children, name = name)
            )
        }

        val head = child(parentName, descriptor, lengths)
        sizingInfoStack.push(head)
    }


    // private fun scheduleCollectionMetaToStack(
    //     name: String,
    //     elementDescriptor: SerialDescriptor,
    //     lengths: IntArray
    // ) {
    //     var lengthIdx = 0
    //     var descriptor = elementDescriptor
    //     // todo should we not throw if getOrNull = null? it means there is no fixed length for this collection.
    //     var element = ElementSizingInfo(numberOfElements = lengths.getOrNull(lengthIdx), name = name)
    //     collectionMetaStack.push(element)
    //     if (descriptor.kind is PrimitiveKind) {
    //         return
    //     }
    //     // todo: are we confident there will be only one element descriptor here by the time we reach this code?
    //     //   i don't see any prior checks, because map is also a collection.
    //     //   this loop is a recursion imitation
    //     descriptor = descriptor.getElementDescriptor(0)
    //     while (descriptor.isCollection()) {
    //         lengthIdx++
    //
    //         element.inner =
    //             listOf(ElementSizingInfo(numberOfElements = lengths.getOrNull(lengthIdx), name = name))
    //         element = element.inner!![0]
    //
    //         descriptor = descriptor.getElementDescriptor(0)
    //     }
    // }



    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        // Unwind sizing meta information for this collection to the stack.
        val collectionMeta = sizingInfoStack.peek()
        check(collectionMeta is SizingInfo.Compound) { "Stack element must describe a compound type" }

        collectionMeta.startByte = getCurrentByteIdx()
        collectionMeta.collectionActualSize = collectionSize

        repeat(collectionSize) {
            collectionMeta.inner
                .filterIsInstance<SizingInfo.Compound>()
                .asReversed()
                .forEach { meta -> sizingInfoStack.push(meta.copy()) }
        }

        // if (descriptor.isCollection) {
        //     collectionMeta.inner?.let { inner ->
        //         repeat(collectionSize) {
        //            inner.asReversed().forEach { meta ->
        //                sizingInfoStack.push(meta.copy())
        //            }
        //        }
        //    }
        // }

        // if (descriptor.kind is StructureKind.LIST) {
        //     collectionMeta.inner?.let { inner ->
        //         repeat(collectionSize) {
        //             collectionMetaStack.push(inner[0].copy())
        //         }
        //     }
        // }

        encodeInt(collectionSize)

        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        when (descriptor.kind) {
            is StructureKind.LIST, StructureKind.MAP -> endCollection(descriptor)
            is StructureKind.CLASS -> {
                sizingInfoStack.pop()
                // val sizingInfo = sizingInfoStack.pop()
                // check(sizingInfo is SizingInfo.Compound) { "Stack element must describe a compound (Class) type" }
                //
                // val currentByteIdx = getCurrentByteIdx()
                // check(sizingInfo.startByte != null) { "Class `${sizingInfo.name}` has no start byte index" }
                // lastStructureSize = currentByteIdx - sizingInfo.startByte!!
            }
            else -> TODO("Unknown structure kind `${descriptor.kind}`")
        }

        super.endStructure(descriptor)
    }

    private fun endCollection(descriptor: SerialDescriptor) {
        val sizingInfo = sizingInfoStack.pop()
        check(sizingInfo is SizingInfo.Compound) { "Stack element must describe a compound (List or Map) type" }
        val (name, startByte, collectionActualSize, collectionRequiredSize, _) = sizingInfo.sizingInfo

        check(startByte != null) { "Structure `$name` has no start byte index" }
        check(collectionActualSize != null) { "Structure `$name` does not specify its actual size" }
        check(collectionRequiredSize != null) { "Structure `$name` does not specify its required size" }


        if (collectionRequiredSize == collectionActualSize) {
            // No padding is required.
            // Save the size of the written structure.
            // lastStructureSize = getCurrentByteIdx() - startByte
            return
        }
        // Collection is to be padded.

        val elementsStartByteIdx = startByte + 4

        // writtenBytes is the amount of bytes corresponding serialization of inner elements.
        val writtenBytes = getCurrentByteIdx() - elementsStartByteIdx

        val elementSize = if (collectionActualSize != 0) {
            writtenBytes / collectionActualSize
        } else {
            getElementSize(descriptor, sizingInfo, serializersModule, defaults)
        }

        repeat(elementSize * (collectionRequiredSize - collectionActualSize)) {
            encodeByte(0)
        }

        // lastStructureSize = elementSize * collectionRequiredSize + 4

        // check(sizingInfo.startByte != null) { "Structure `${sizingInfo.name}` has no start byte index" }
        // val elementsStartByteIdx = sizingInfo.startByte!! + 4
        //
        // // writtenBytes is the amount of bytes corresponding serialization of inner elements.
        // val writtenBytes = getCurrentByteIdx() - elementsStartByteIdx
        //
        //
        // val expectedNumberOfElements = sizingInfo.collectionRequiredSize
        // check(expectedNumberOfElements != null) { "List `${descriptor.serialName}` must have DFLength annotation" }
        //
        // val elementSize = lastStructureSize ?: getElementSize(descriptor, serializersModule, defaults)
        // val expectedBytes = elementSize * expectedNumberOfElements
        //
        // val paddingBytesLength = expectedBytes - writtenBytes
        // repeat(paddingBytesLength) { encodeByte(0) }
        //
        // lastStructureSize = expectedBytes + 4
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

        val sizingInfo = sizingInfoStack.pop()
        check(sizingInfo is SizingInfo.Compound) { "Stack element must describe a compound (String) type" }

        val expectedStringLength = sizingInfo.collectionRequiredSize
        check(expectedStringLength != null) { "Strings should have @ValueLength annotation" }

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