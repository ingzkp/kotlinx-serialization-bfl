package com.ing.serialization.bfl.serde

import com.ing.serialization.bfl.serde.element.CollectionElement
import com.ing.serialization.bfl.serde.element.EnumElement
import com.ing.serialization.bfl.serde.element.PolymorphicStructureElement
import com.ing.serialization.bfl.serde.element.PrimitiveElement
import com.ing.serialization.bfl.serde.element.StringElement
import com.ing.serialization.bfl.serializers.BFLSerializers
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import java.io.DataInput

@Suppress("TooManyFunctions")
class BinaryFixedLengthInputDecoder(
    private val input: DataInput,
    userSerializersModule: SerializersModule,
    private val outerFixedLength: IntArray
) : AbstractDecoder() {
    override val serializersModule = BFLSerializers + userSerializersModule

    private var elementIndex = 0
    private lateinit var structureProcessor: FixedLengthStructureProcessor

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        if (!descriptor.isCollection) {
            structureProcessor.beginStructure(descriptor)
        }
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        when {
            descriptor.isCollection -> endCollection()
            descriptor.isStructure || descriptor.isPolymorphic || descriptor.isObject -> structureProcessor.removeNext()
            else -> TODO("Unknown structure kind `${descriptor.kind}`")
        }
    }

    private fun endCollection() {
        // it's a crutch, but because of enabled sequential decoding (due to performance reasons),
        // key-value (aka Pair instances) serializers don't call endStructure() on elements of list-like structures
        while (structureProcessor.peekNext() !is CollectionElement) {
            structureProcessor.removeNext()
        }

        val collection = structureProcessor
            .removeNext()
            .expect<CollectionElement>()

        // collection might have been padded.
        input.skipBytes(collection.padding)
    }

    override fun decodeBoolean() = structureProcessor.removeNext().expect<PrimitiveElement>().decode<Boolean>(input)
    override fun decodeByte() = structureProcessor.removeNext().expect<PrimitiveElement>().decode<Byte>(input)
    override fun decodeShort() = structureProcessor.removeNext().expect<PrimitiveElement>().decode<Short>(input)
    override fun decodeInt() = structureProcessor.removeNext().expect<PrimitiveElement>().decode<Int>(input)
    override fun decodeLong() = structureProcessor.removeNext().expect<PrimitiveElement>().decode<Long>(input)
    override fun decodeChar() = structureProcessor.removeNext().expect<PrimitiveElement>().decode<Char>(input)
    override fun decodeString() = structureProcessor.removeNext().expect<StringElement>().decode(input)
    override fun decodeEnum(enumDescriptor: SerialDescriptor) = structureProcessor.removeNext().expect<EnumElement>().decode(input)
    override fun decodeFloat() = structureProcessor.removeNext().expect<PrimitiveElement>().decode<Float>(input)
    override fun decodeDouble() = structureProcessor.removeNext().expect<PrimitiveElement>().decode<Double>(input)

    override fun decodeSequentially(): Boolean = true

    override fun decodeCollectionSize(descriptor: SerialDescriptor) =
        input.readInt().also { structureProcessor.beginCollection(it) }

    override fun decodeElementIndex(descriptor: SerialDescriptor) =
        if (elementIndex == structureProcessor.peekNext().expect<CollectionElement>().actualLength) {
            CompositeDecoder.DECODE_DONE
        } else {
            elementIndex++
        }

    override fun decodeNotNullMark() = input.readBoolean()

    override fun decodeNull(): Nothing? {
        structureProcessor.removeNext().let {
            if (it is PolymorphicStructureElement) it.decodeNull(input, serializersModule)
            else it.decodeNull(input)
        }
        return null
    }

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        if (!this::structureProcessor.isInitialized) {
            structureProcessor =
                FixedLengthStructureProcessor(deserializer.descriptor, serializersModule, outerFixedLength, phase = Phase.DECODING)
        }
        return super.decodeSerializableValue(deserializer)
    }
}
