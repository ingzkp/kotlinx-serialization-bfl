
import decoder.DataInputDecoder
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import serde.IndexedDataOutputEncoder
import serde.Size
import serde.SizingInfo
import java.io.DataInput
import java.io.DataOutput

@ExperimentalSerializationApi
fun <T: Any> encodeTo(output: DataOutput, serializer: SerializationStrategy<T>, value: T, serializersModule: SerializersModule, vararg defaults: Any) =
    IndexedDataOutputEncoder(output, serializersModule, defaults).encodeSerializableValue(serializer, value)

@ExperimentalSerializationApi
inline fun <reified T: Any> encodeTo(output: DataOutput, value: T, serializersModule: SerializersModule, vararg defaults: Any) =
    encodeTo(output, serializer(), value, serializersModule, defaults.toList())

@ExperimentalSerializationApi
fun <T> decodeFrom(input: DataInput, deserializer: DeserializationStrategy<T>): T =
    DataInputDecoder(input).decodeSerializableValue(deserializer)

@ExperimentalSerializationApi
inline fun <reified T> decodeFrom(input: DataInput): T = decodeFrom(input, serializer())

@ExperimentalUnsignedTypes
fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').toUpperCase()}}"
}

// TODO Ultimately this function must only belong to the encoder only
fun getElementSize(
    descriptor: SerialDescriptor,
    sizingInfo: SizingInfo,
    serializersModule: SerializersModule, vararg defaults: Any
): Int =
    // TODO have a better look here, is descriptor always decomposable in primitive types?
    //   no it is not, it can also be a list or map or a class.
    runCatching {
        // todo send the string representation of type there somehow
        //  serialName can be overridden, otherwise it coincides with the fully-qualified name
        Size.of(descriptor.serialName, serializersModule, defaults)
    }.getOrElse {
        when (descriptor.kind) {
            is PrimitiveKind.BOOLEAN -> 1
            is PrimitiveKind.BYTE -> 1
            is PrimitiveKind.SHORT -> 2
            is PrimitiveKind.INT -> 4
            is PrimitiveKind.LONG -> 8
            is PrimitiveKind.FLOAT -> throw IllegalStateException("Floats are not yet supported")
            is PrimitiveKind.DOUBLE -> throw IllegalStateException("Double are not yet supported")
            is PrimitiveKind.CHAR -> 2
            is PrimitiveKind.STRING -> {
                // SHORT (string length) + number_of_elements * CHAR = 2 + n * 2
                check(sizingInfo is SizingInfo.Compound ) { "Sizing information on Strings must be present" }

                val n = sizingInfo.collectionRequiredSize
                check(n != null) { "Sizes of Strings must be known: ${sizingInfo.name}" }

                2 + n * 2
            }

            is StructureKind.LIST, StructureKind.MAP -> {
                // INT (collection length) + number_of_elements * sum_i { size(inner_i) }
                // = 4 + n * sum_i { size(inner_i) }

                check(sizingInfo is SizingInfo.Compound) { "Sizing information on Collections must be present" }

                check(sizingInfo.inner.size == descriptor.elementsCount)
                    { "Sizing info does not coincide with descriptors"}

                val innerSize = sizingInfo.inner.zip(descriptor.elementDescriptors).sumBy { (childSizingInfo, childDescriptor) ->
                    getElementSize(childDescriptor, childSizingInfo, serializersModule, defaults)
                }

                val n = sizingInfo.collectionRequiredSize
                check(n != null) { "Sizes of List-like structures must be known: ${sizingInfo.name}" }

                4 + n * innerSize
            }
            //
            else -> {
                check(sizingInfo is SizingInfo.Compound) { "Sizing information on Collections must be present" }

                check(sizingInfo.inner.size == descriptor.elementsCount)
                { "Sizing info does not coincide with descriptors"}

                sizingInfo.inner.zip(descriptor.elementDescriptors).sumBy { (childSizingInfo, childDescriptor) ->
                    getElementSize(childDescriptor, childSizingInfo, serializersModule, defaults)
                }
            }
        }
    }