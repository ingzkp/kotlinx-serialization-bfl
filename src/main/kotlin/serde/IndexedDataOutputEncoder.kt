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
    private val defaults: List<Any>
) : AbstractEncoder() {

    private val elementStack: ArrayDeque<Element> = ArrayDeque()
    private var topLevelProcessing = true

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        if (topLevelProcessing) {
            elementStack.push(Element.Structure(descriptor.serialName))

            // Field processing only makes sense
            // This will be called for the top structure and this is the only place where annotations are accessible.
            descriptor.elementDescriptors.reversed()
                .forEachIndexed { idx, childDesc ->
                    when {
                        childDesc.isCollectionOrString -> {
                            // Top-level Collection or String must have annotations
                            val lengths = descriptor.getElementAnnotations(idx)
                                .filterIsInstance<DFLength>()
                                .firstOrNull()?.lengths?.toMutableList()
                                ?: error("Element ${childDesc.serialName} must have DFLength annotation")

                            scheduleCompoundToStack(descriptor.serialName, childDesc, lengths)
                        }
                        childDesc.isStructure -> {
                            // Classes may have inner collections buried deep inside.
                            // We can infer this by observing an appropriate length annotation.
                            val lengths = descriptor.getElementAnnotations(idx)
                                .filterIsInstance<DFLength>()
                                .firstOrNull()?.lengths?.toMutableList()
                            if (lengths != null) {
                                scheduleCompoundToStack(descriptor.serialName, childDesc, lengths)
                            }
                        }
                    }
                }

            topLevelProcessing = false
        } else {
            val structureMeta = elementStack.peek()
            check(structureMeta is Element.Structure) { "Stack element must describe a structure type" }

            if (structureMeta.inner.isEmpty()){
                elementStack.push(Element.Structure(descriptor.serialName))
            }

            // Structure inner elements need to be unwound on the stack.
            structureMeta.inner
                .filter { it !is Element.Primitive }
                .asReversed()
                .forEach { meta -> elementStack.push(meta.copy()) }
        }

        // pushStructureMetaToStack(descriptor)
        // processStructureFields(descriptor)
        //
        // // This holds by stack construction.
        // (elementStack.peek() as Element.Compound).startByte = getCurrentByteIdx()

        return super.beginStructure(descriptor)
    }

    // private fun pushStructureMetaToStack(descriptor: SerialDescriptor) =
    //     elementStack.push(Element.Compound(
    //         CollectedSizingInfo(startByte = getCurrentByteIdx(), name = descriptor.serialName)
    //     ))

    // private fun processStructureFields(descriptor: SerialDescriptor) =
    //     (descriptor.elementsCount - 1 downTo 0)
    //         // .filter { descriptor.getElementDescriptor(it).mustBeAnnotated() }
    //         .filter { descriptor.getElementDescriptor(it).isCollected }
    //         .forEach {
    //             val name = "${descriptor.serialName}.${descriptor.getElementName(it)}"
    //             val annotations = descriptor.getElementAnnotations(it)
    //
    //             // DFLength must always be present for all unbounded types.
    //             val dfLength = annotations
    //                 .filterIsInstance<DFLength>()
    //                 .firstOrNull()?.lengths
    //                 ?: error("Element $name must have DFLength annotation")
    //
    //             scheduleCollectionMetaToStack(
    //                 descriptor.serialName,
    //                 descriptor.getElementDescriptor(it),
    //                 dfLength.toMutableList()
    //             )
    //         }



    // // todo get back to using this after the inner question is addressed
    // private fun SerialDescriptor.mustBeAnnotated() =
    //     kind is StructureKind.LIST
    //         || kind is StructureKind.MAP
    //         || kind is PrimitiveKind.STRING
    //         // todo; explain better why the last two are required.
    //         || kind is PolymorphicKind
    //         || kind is SerialKind.CONTEXTUAL

    private fun scheduleCompoundToStack(
        parentName: String,
        descriptor: SerialDescriptor,
        lengths: MutableList<Int>
    ) {
        fun child(parentName: String, descriptor: SerialDescriptor, lengths: MutableList<Int>): Element {
            val name = "$parentName.${descriptor.serialName}"

            if (descriptor.isPrimitive) {
                return Element.Primitive(name)
            }

            if (descriptor.isCollectionOrString) {
                val requiredSize = lengths.removeFirstOrNull() ?: error("Insufficient sizing info for $name")
                val children = descriptor.elementDescriptors.map {
                    child(name, it, lengths)
                }
                return Element.Collection(name,
                    CollectedSizingInfo(collectionRequiredSize = requiredSize, inner = children)
                )
            }

            if (descriptor.isStructure) {
                val children = descriptor.elementDescriptors.map {
                    child(name, it, lengths)
                }
                return Element.Structure(name, inner = children)
            }

            error("Unreachable code")
        }

        val head = child(parentName, descriptor, lengths)
        elementStack.push(head)
    }

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        // Unwind sizing meta information for this collection to the stack.
        val collectionMeta = elementStack.peek()
        check(collectionMeta is Element.Collection) { "Stack element must describe a collection type" }

        collectionMeta.startByte = getCurrentByteIdx()
        collectionMeta.collectionActualSize = collectionSize

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
            else -> TODO("Unknown structure kind `${descriptor.kind}`")
        }

        super.endStructure(descriptor)
    }

    private fun endCollection(descriptor: SerialDescriptor) {
        val sizingInfo = elementStack.pop()
        check(sizingInfo is Element.Collection) { "Stack element must describe a compound (List or Map) type" }
        val name = sizingInfo.name
        val (startByte, collectionActualSize, collectionRequiredSize, innerSizingInfo) = sizingInfo.sizingInfo

        check(startByte != null) { "Structure `$name` has no start byte index" }
        check(collectionActualSize != null) { "Structure `$name` does not specify its actual size" }
        check(collectionRequiredSize != null) { "Structure `$name` does not specify its required size" }

        if (collectionRequiredSize == collectionActualSize) {
            // No padding is required.
            // Save the size of the written structure.
            // lastStructureSize = getCurrentByteIdx() - startByte
            return
        }
        // Compound is to be padded.

        val elementsStartByteIdx = startByte + 4

        // writtenBytes is the amount of bytes corresponding serialization of inner elements.
        val writtenBytes = getCurrentByteIdx() - elementsStartByteIdx

        val elementSize = if (collectionActualSize != 0) {
            writtenBytes / collectionActualSize
        } else {
            check(innerSizingInfo.size == descriptor.elementsCount) { "Sizing info does not coincide with descriptors"}

            innerSizingInfo.zip(descriptor.elementDescriptors).sumBy { (childSizingInfo, childDescriptor) ->
                getElementSize(childDescriptor, childSizingInfo, serializersModule, defaults)
            }
        }

        repeat(elementSize * (collectionRequiredSize - collectionActualSize)) {
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
        check(sizingInfo is Element.Collection) { "Stack element must describe a compound (String) type" }

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

    private val SerialDescriptor.isCollection: Boolean
        get() = kind is StructureKind.LIST || kind is StructureKind.MAP

    private val SerialDescriptor.isCollectionOrString: Boolean
        get() = isCollection || kind is PrimitiveKind.STRING

    private val SerialDescriptor.isPrimitive: Boolean
        get() = kind is PrimitiveKind && kind !is PrimitiveKind.STRING

    private val SerialDescriptor.isStructure: Boolean
        get() = kind is StructureKind.CLASS
}