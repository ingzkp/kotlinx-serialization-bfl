package com.ing.serialization.bfl.serde

import com.ing.serialization.bfl.serde.element.CollectionElement
import com.ing.serialization.bfl.serde.element.PrimitiveElement
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
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
    val layout by lazy { structureProcessor.structure.layout }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        structureProcessor.beginStructure(descriptor)
        return this
    }

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        structureProcessor.beginCollection(collectionSize)
        output.writeInt(collectionSize)
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        when {
            descriptor.isCollection -> {
                val collection = structureProcessor
                    .removeNext()
                    .expect<CollectionElement>()
                repeat(collection.padding) { output.writeByte(0) }
            }
            descriptor.isStructure || descriptor.isPolymorphic -> {
                structureProcessor.removeNext()
            }
            else -> TODO("Unknown structure kind `${descriptor.kind}`")
        }
    }

    override fun encodeBoolean(value: Boolean) = structureProcessor.removeNext().expect<PrimitiveElement>().encode(output, value)
    override fun encodeByte(value: Byte) = structureProcessor.removeNext().expect<PrimitiveElement>().encode(output, value)
    override fun encodeShort(value: Short) = structureProcessor.removeNext().expect<PrimitiveElement>().encode(output, value)
    override fun encodeInt(value: Int) = structureProcessor.removeNext().expect<PrimitiveElement>().encode(output, value)
    override fun encodeLong(value: Long) = structureProcessor.removeNext().expect<PrimitiveElement>().encode(output, value)
    override fun encodeChar(value: Char) = structureProcessor.removeNext().expect<PrimitiveElement>().encode(output, value)
    override fun encodeString(value: String) = structureProcessor.removeNext().expect<StringElement>().encode(value, output)
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = output.writeInt(index)

    override fun encodeFloat(value: Float) = throw SerdeError.UnsupportedPrimitive(PrimitiveKind.FLOAT)
    override fun encodeDouble(value: Double) = throw SerdeError.UnsupportedPrimitive(PrimitiveKind.DOUBLE)

    override fun encodeNull() {
        output.writeBoolean(false)
        structureProcessor.removeNext().encodeNull(output)
    }

    override fun encodeNotNullMark() = output.writeBoolean(true)
}
