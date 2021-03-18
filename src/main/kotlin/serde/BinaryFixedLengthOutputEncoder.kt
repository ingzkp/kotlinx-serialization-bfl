package serde

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule
import java.io.DataOutput

@Suppress("TooManyFunctions")
@ExperimentalSerializationApi
class BinaryFixedLengthOutputEncoder(
    private val output: DataOutput,
    override val serializersModule: SerializersModule
) : AbstractEncoder() {
    private val structureProcessor = FixedLengthStructureProcessor(serializersModule)

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        structureProcessor.beginStructure(descriptor)
        return this
    }

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        structureProcessor.beginCollection(collectionSize)
        encodeInt(collectionSize)
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        when {
            descriptor.isCollection -> {
                val collection = structureProcessor.removeNextProcessed().expect<Element.Collection>()
                repeat(collection.padding) { encodeByte(0) }
            }
            descriptor.isStructure || descriptor.isPolymorphic -> structureProcessor.removeNextProcessed()
            else -> TODO("Unknown structure kind `${descriptor.kind}`")
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
        val string = structureProcessor.removeNextProcessed().expect<Element.Strng>()
        val actualLength = value.length

        // In output.writeUTF, length of the string is stored as short.
        // We do the same for consistency.
        encodeShort(value.length.toShort())
        value.forEach { encodeChar(it) }

        repeat(string.padding(actualLength)) { encodeByte(0) }
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = output.writeInt(index)

    override fun encodeNull() = encodeBoolean(false)
    override fun encodeNotNullMark() = encodeBoolean(true)
}