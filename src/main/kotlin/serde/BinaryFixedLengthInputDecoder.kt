package serde

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule
import java.io.DataInput

@ExperimentalSerializationApi
class BinaryFixedLengthInputDecoder(
    private val input: DataInput,
    override val serializersModule: SerializersModule
) : AbstractDecoder() {
    private var elementIndex = 0
    private val structureProcessor = FixedLengthStructureProcessor(serializersModule)

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        if (!descriptor.isCollection) {
            structureProcessor.beginStructure(descriptor)
        }
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        when {
            descriptor.isCollection -> endCollection()
            descriptor.isStructure || descriptor.isPolymorphic -> structureProcessor.removeNextProcessed()
            else -> TODO("Unknown structure kind `${descriptor.kind}`")
        }
    }

    private fun endCollection() {
        // it's a crutch, but because of enabled sequential decoding (due to performance reasons),
        // key-value (aka Pair instances) serializers don't call endStructure() on elements of list-like structures
        while (structureProcessor.getNextProcessed() !is Element.Collection) {
            structureProcessor.removeNextProcessed()
        }

        // Collection might have been padded.
        input.skipBytes(structureProcessor.collectionPadding)
    }

    override fun decodeString(): String {
        // In output.writeUTF, length of the string is stored as short.
        // We do the same for consistency.
        val actualLength = decodeShort()
        val value = (0 until actualLength).map { decodeChar() }.joinToString("")

        input.skipBytes(structureProcessor.stringPadding(actualLength.toInt()))

        return value
    }

    override fun decodeBoolean(): Boolean = input.readBoolean()
    override fun decodeByte() = input.readByte()
    override fun decodeShort() = input.readShort()
    override fun decodeInt() = input.readInt()
    override fun decodeLong() = input.readLong()
    override fun decodeFloat() = input.readFloat()
    override fun decodeDouble() = input.readDouble()
    override fun decodeChar() = input.readChar()
    override fun decodeEnum(enumDescriptor: SerialDescriptor) = input.readInt()

    override fun decodeSequentially(): Boolean = true

    override fun decodeCollectionSize(descriptor: SerialDescriptor) =
        decodeInt().also { structureProcessor.beginCollection(it) }

    override fun decodeElementIndex(descriptor: SerialDescriptor) =
        if (elementIndex == structureProcessor.getNextProcessed().expect<Element.Collection>().actualLength) {
            CompositeDecoder.DECODE_DONE
        } else {
            elementIndex++
        }

    override fun decodeNotNullMark(): Boolean = decodeBoolean()
}