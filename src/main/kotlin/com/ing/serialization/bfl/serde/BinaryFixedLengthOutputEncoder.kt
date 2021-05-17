package com.ing.serialization.bfl.serde

import com.ing.serialization.bfl.serde.element.CollectionElement
import com.ing.serialization.bfl.serde.element.EnumElement
import com.ing.serialization.bfl.serde.element.PrimitiveElement
import com.ing.serialization.bfl.serde.element.StringElement
import com.ing.serialization.bfl.serializers.BFLSerializers
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import java.io.DataOutput

@Suppress("TooManyFunctions")
class BinaryFixedLengthOutputEncoder(
    private val output: DataOutput,
    userSerializersModule: SerializersModule,
    private val outerFixedLength: IntArray
) : AbstractEncoder() {
    override val serializersModule = BFLSerializers + userSerializersModule

    private lateinit var structureProcessor: FixedLengthStructureProcessor
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
            descriptor.isStructure || descriptor.isPolymorphic || descriptor.isObject -> {
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
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = structureProcessor.removeNext().expect<EnumElement>().encode(index, output)
    override fun encodeFloat(value: Float) = structureProcessor.removeNext().expect<PrimitiveElement>().encode(output, value)
    override fun encodeDouble(value: Double) = structureProcessor.removeNext().expect<PrimitiveElement>().encode(output, value)

    override fun encodeNull() {
        output.writeBoolean(false)
        // before encoding a null value, we need to know whether it was fully resolved during parsing (case of nullable
        // polymorphic elements within the structure) - if not, an exception is thrown
        structureProcessor.removeNext().verifyResolvabilityOrThrow().encodeNull(output)
    }

    override fun encodeNotNullMark() = output.writeBoolean(true)

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        if (!this::structureProcessor.isInitialized) {
            structureProcessor =
                FixedLengthStructureProcessor(serializer.descriptor, serializersModule, outerFixedLength, value, phase = Phase.ENCODING)
        }
        super.encodeSerializableValue(serializer, value)
    }
}
