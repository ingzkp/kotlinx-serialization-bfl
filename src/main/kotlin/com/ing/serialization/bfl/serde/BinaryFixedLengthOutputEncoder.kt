package com.ing.serialization.bfl.serde

import com.ing.serialization.bfl.serde.element.CollectionElement
import com.ing.serialization.bfl.serde.element.PrimitiveElement
import com.ing.serialization.bfl.serde.element.StringElement
import com.ing.serialization.bfl.serializers.BFLSerializers
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import java.io.DataOutput

@Suppress("TooManyFunctions")
class BinaryFixedLengthOutputEncoder(
    private val output: DataOutput,
    userSerializersModule: SerializersModule
) : AbstractEncoder() {
    override val serializersModule = BFLSerializers + userSerializersModule

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
    override fun encodeFloat(value: Float) = structureProcessor.removeNext().expect<PrimitiveElement>().encode(output, value)
    override fun encodeDouble(value: Double) = structureProcessor.removeNext().expect<PrimitiveElement>().encode(output, value)

    override fun encodeNull() {
        output.writeBoolean(false)
        structureProcessor.removeNext().encodeNull(output)
    }

    override fun encodeNotNullMark() = output.writeBoolean(true)
}
