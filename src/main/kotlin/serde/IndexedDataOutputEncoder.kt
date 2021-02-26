package serde

import annotations.FixedLength
import getElementSize
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import serializers.RSAPublicKeySerializer
import sun.security.rsa.RSAPublicKeyImpl
import java.io.DataOutput
import java.io.DataOutputStream
import java.security.PublicKey

@ExperimentalSerializationApi
class IndexedDataOutputEncoder(private val output: DataOutput, private val defaults: List<Any>) : AbstractEncoder() {

    private val collections = mutableMapOf<SerialDescriptor, CollectionMeta>()

    override val serializersModule: SerializersModule = SerializersModule {
        polymorphic(PublicKey::class) {
            subclass(RSAPublicKeyImpl::class, RSAPublicKeySerializer)
        }
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        descriptor.elementDescriptors
            .forEachIndexed { idx, child ->
                when (child.kind) {
                    StructureKind.LIST -> {
                        collections[child] = CollectionMeta(
                            start = null,
                            occupies = null,
                            descriptor.getElementAnnotations(idx),
                            mutableMapOf("field" to descriptor.getElementName(idx))
                        )
                    }
                    PrimitiveKind.STRING -> {
                        collections[child] = CollectionMeta(
                            start = null,
                            occupies = null,
                            descriptor.getElementAnnotations(idx),
                            mutableMapOf("field" to descriptor.getElementName(idx))
                        )
                    }
                    StructureKind.MAP -> TODO("Implement map support")
                    else -> println("Ignored field ${descriptor.serialName}.${child.serialName}")
                }
            }

        return super.beginStructure(descriptor)
    }

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        encodeInt(collectionSize)
        collections[descriptor]?.start = output.getCurrentByteIdx()
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        when (descriptor.kind) {
            StructureKind.LIST -> with (collections[descriptor]) {
                // Not removing this metadata because it may be handy for treating nested lists.
                this ?: error("Collection must have meta information")
                start ?: error("Starting position for the collection elements must known")

                occupies = finalizeCollection(descriptor, annotations, start!!) + 4
                free["processed"] = true
            }
            StructureKind.MAP -> TODO("Implement map support")
            else -> println("Ignored field ${descriptor.serialName}")
        }

        super.endStructure(descriptor)
    }

    private fun finalizeCollection(descriptor: SerialDescriptor, annotations: List<Annotation>, startIdx: Int): Int {
        val expectedNumberOfElements = annotations
            .filterIsInstance<FixedLength>()
            .firstOrNull()?.values?.firstOrNull()

        require(expectedNumberOfElements != null) {
            "Collection `${descriptor.serialName}` must have FixedLength annotation"
        }

        val expectedLength = expectedNumberOfElements * getElementSize(descriptor.elementDescriptors.single(), defaults)

        val currentByteIdx = output.getCurrentByteIdx()
        val actualLength = currentByteIdx - startIdx
        require(expectedLength > actualLength) {
            "Serialized elements don't fit into their expected length"
        }

        repeat(expectedLength - actualLength) { encodeByte(5) }

        return expectedLength
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

        val collectionMeta = collections[String.serializer().descriptor]!!
        val expectedStringLength = collectionMeta.annotations.filterIsInstance<FixedLength>().firstOrNull()?.values?.firstOrNull()

        check(expectedStringLength != null) { "Strings should have @FixedLength annotation" }

        val paddingStringLength = expectedStringLength - actualStringLength
        val paddedBytesLength = paddingStringLength * getElementSize(Char.serializer().descriptor, defaults)

        repeat(paddedBytesLength) { encodeByte(0) }
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = output.writeInt(index)

    override fun encodeNull() = encodeBoolean(false)
    override fun encodeNotNullMark() = encodeBoolean(true)

    private fun DataOutput.getCurrentByteIdx(): Int = (this as DataOutputStream).size()
}