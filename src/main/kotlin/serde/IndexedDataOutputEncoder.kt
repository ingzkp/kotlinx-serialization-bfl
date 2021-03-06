package serde

import annotations.DFLength
import getElementSize
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
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
                scheduleCompoundToStack(descriptor.serialName, descriptor)
                unwindStructureToStack(descriptor)
            } else {
                (descriptor.elementsCount - 1 downTo 0).forEach { idx ->
                    scheduleElementToStack(descriptor, idx)
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

    private fun scheduleElementToStack(parentDescriptor: SerialDescriptor, elementIdx: Int) {
        val descriptor = parentDescriptor.getElementDescriptor(elementIdx)

        when {
            descriptor.isCollection || descriptor.isString -> {
                // Top-level Collection or String must have annotations
                val lengths = parentDescriptor.getElementAnnotations(elementIdx)
                    .filterIsInstance<DFLength>()
                    .firstOrNull()?.lengths?.toMutableList()
                    ?: error("Element ${descriptor.serialName} must have DFLength annotation")

                scheduleCompoundToStack(parentDescriptor.serialName, descriptor, lengths)
            }
            descriptor.isStructure -> {
                // Classes may have inner collections buried deep inside.
                // We can infer this by observing an appropriate length annotation.
                val lengths = parentDescriptor.getElementAnnotations(elementIdx)
                    .filterIsInstance<DFLength>()
                    .firstOrNull()?.lengths?.toMutableList()
                if (lengths != null) {
                    scheduleCompoundToStack(parentDescriptor.serialName, descriptor, lengths)
                } else {
                    elementStack.push(Element.Structure(
                        "${parentDescriptor.serialName}.${descriptor.serialName}",
                        isResolved = false
                    ))
                }
            }
            descriptor.isPolymorphic || descriptor.isContextual -> {
                scheduleCompoundToStack(parentDescriptor.serialName, descriptor)
            }
        }
    }

    private fun scheduleCompoundToStack(
        parentName: String,
        descriptor: SerialDescriptor,
        lengths: MutableList<Int> = mutableListOf()
    ) {
        fun child(parentName: String, descriptor: SerialDescriptor, lengths: MutableList<Int>): Element {
            val name = "$parentName.${descriptor.serialName}"

            return when {
                descriptor.isTrulyPrimitive -> Element.Primitive(name)
                descriptor.isCollection || descriptor.isString -> {
                    val requiredSize = lengths.removeFirstOrNull() ?: error("Insufficient sizing info for $name")
                    val children = descriptor.elementDescriptors.map {
                        child(name, it, lengths)
                    }
                    Element.Collection(name,
                        CollectedSizingInfo(collectionRequiredSize = requiredSize, inner = children)
                    )
                }
                descriptor.isStructure ->  {
                    val children = descriptor.elementDescriptors.map {
                        child(name, it, lengths)
                    }
                    Element.Structure(name, inner = children, isResolved = true)
                }
                descriptor.isPolymorphic -> {
                    lengths.add(0, 100)
                    val children = descriptor.elementDescriptors.map {
                        child(name, it, lengths)
                    }
                    Element.Structure(name, inner = children, isResolved = true)
                }
                descriptor.isContextual -> Element.Structure(name, isResolved = false)
                else -> error("Unreachable code when building child ${descriptor.serialName}")
            }
        }

        var head = child(parentName, descriptor, lengths)
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
            is PolymorphicKind -> elementStack.pop()
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
            return
        }
        // Collection is to be padded.

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

    private fun <T> ArrayDeque<T>.push(value: T) = this.addFirst(value)
    private fun <T> ArrayDeque<T>.pop(): T = this.removeFirst()
    private fun <T> ArrayDeque<T>.peek(): T = this.first()
    private fun <T> ArrayDeque<T>.peekOrNull(): T? = this.firstOrNull()

    private val SerialDescriptor.isCollection: Boolean
        get() = kind is StructureKind.LIST || kind is StructureKind.MAP

    private val SerialDescriptor.isString: Boolean
        get() = kind is PrimitiveKind.STRING

    private val SerialDescriptor.isTrulyPrimitive: Boolean
        get() = kind is PrimitiveKind && kind !is PrimitiveKind.STRING

    private val SerialDescriptor.isStructure: Boolean
        get() = kind is StructureKind.CLASS

    private val SerialDescriptor.isPolymorphic: Boolean
        get() = kind is PolymorphicKind

    private val SerialDescriptor.isContextual: Boolean
        get() = kind is SerialKind.CONTEXTUAL
}