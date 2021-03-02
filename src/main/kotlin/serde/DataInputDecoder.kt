package decoder

import annotations.FixedLength
import getElementSize
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import serde.ElementSerializingMeta
import serializers.RSAPublicKeySerializer
import sun.security.rsa.RSAPublicKeyImpl
import java.io.DataInput
import java.security.PublicKey

data class DeserializationState(var byteIndex: Int, val collections: MutableMap<SerialDescriptor, ElementSerializingMeta>)

@ExperimentalSerializationApi
class DataInputDecoder(
    private val input: DataInput,
    var elementsCount: Int = 0,
    private val deserializationState: DeserializationState = DeserializationState(0, mutableMapOf()),
    private val defaults: List<Any> = emptyList()
) : AbstractDecoder() {
    private var elementIndex = 0

    override val serializersModule: SerializersModule = SerializersModule {
        polymorphic(PublicKey::class) {
            subclass(RSAPublicKeyImpl::class, RSAPublicKeySerializer)
        }
    }

    override fun decodeBoolean(): Boolean {
        deserializationState.byteIndex++
        return input.readByte().toInt() != 0
    }

    override fun decodeByte()= input.readByte().also { deserializationState.byteIndex++ }
    override fun decodeShort() = input.readShort().also { deserializationState.byteIndex += 2 }
    override fun decodeInt() = input.readInt().also { deserializationState.byteIndex += 4 }
    override fun decodeLong() = input.readLong().also { deserializationState.byteIndex += 8 }
    override fun decodeFloat() = input.readFloat().also { deserializationState.byteIndex += 4 }
    override fun decodeDouble() = input.readDouble().also { deserializationState.byteIndex += 8 }
    override fun decodeChar() = input.readChar().also { deserializationState.byteIndex += 2 }

    override fun decodeString(): String {
        val actualStringLength = decodeShort()
        val string = (0 until actualStringLength).map { decodeChar() }.joinToString("")

        val collectionMeta = deserializationState.collections[String.serializer().descriptor]!!
        val expectedStringLength = collectionMeta.numberOfElements!!

        //TODO: I removed check, but it was useful to prevent user from missing non-annotated list-like structures

        val paddingStringLength = expectedStringLength - actualStringLength
        val paddedBytesLength = paddingStringLength * getElementSize(Char.serializer().descriptor, serializersModule, defaults)

        input.skipBytes(paddedBytesLength)
        deserializationState.byteIndex += paddedBytesLength

        return string
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        deserializationState.byteIndex += 4
        return input.readInt()
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == elementsCount) return CompositeDecoder.DECODE_DONE
        return elementIndex++
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
//        descriptor.elementDescriptors
//            .forEachIndexed { idx, child ->
//                when (child.kind) {
//                    StructureKind.LIST -> {
//                        deserializationState.collections[child] = ElementSizingInfo(
//                            start = deserializationState.byteIndex + 4,
//                            occupies = null,
//                            descriptor.getElementAnnotations(idx),
//                            mutableMapOf("field" to descriptor.getElementName(idx))
//                        )
//                    }
//                    PrimitiveKind.STRING -> {
//                        deserializationState.collections[child] = ElementSizingInfo(
//                            start = deserializationState.byteIndex,
//                            occupies = null,
//                            descriptor.getElementAnnotations(idx),
//                            mutableMapOf("field" to descriptor.getElementName(idx))
//                        )
//                    }
//                    StructureKind.MAP -> TODO("Implement map support")
//                    else -> println("Ignored field ${descriptor.serialName}.${child.serialName}")
//                }
//            }
//
//        val meta = deserializationState.collections[descriptor]
//        if (meta != null && descriptor.kind == StructureKind.LIST) {
//            meta.start = deserializationState.byteIndex + 4
//        }

        return DataInputDecoder(input, descriptor.elementsCount, deserializationState, defaults)
    }

    override fun endStructure(descriptor: SerialDescriptor) {
//        when (descriptor.kind) {
//            StructureKind.LIST -> with (deserializationState.collections[descriptor]) {
//                // Not removing this metadata because it may be handy for treating nested lists.
//                this ?: error(" Something doesn't add up")
//
//                occupies = finalizeCollection(descriptor, annotations, start ?: error ("Wait a minute. Hang on a second."))
//                free["processed"] = true
//            }
//            StructureKind.MAP -> TODO("Implement map support")
//            else -> println("Ignored field ${descriptor.serialName}")
//        }

        super.endStructure(descriptor)
    }

    private fun finalizeCollection(descriptor: SerialDescriptor, annotations: List<Annotation>, startIdx: Int): Int {
        val expectedNumberOfElements = annotations
            .filterIsInstance<FixedLength>()
            .firstOrNull()?.lengths?.firstOrNull()

        require(expectedNumberOfElements != null) {
            "Collection `${descriptor.serialName}` must have FixedLength annotation"
        }

        val expectedLength = expectedNumberOfElements * getElementSize(descriptor.elementDescriptors.single(), serializersModule, defaults)

        val currentByteIdx = deserializationState.byteIndex
        val actualLength = currentByteIdx - startIdx
        require(expectedLength > actualLength) {
            "Serialized elements don't fit into their expected length"
        }

        val paddedBytesLength = expectedLength - actualLength
        input.skipBytes(paddedBytesLength)
        deserializationState.byteIndex += paddedBytesLength


        return expectedLength
    }

    override fun decodeSequentially(): Boolean = true

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int =
        decodeInt().also { elementsCount = it }

    override fun decodeNotNullMark(): Boolean = decodeBoolean()
}