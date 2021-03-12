import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import serde.*
import java.io.DataInput
import java.io.DataOutput

@ExperimentalSerializationApi
fun <T: Any> encodeTo(output: DataOutput, serializer: SerializationStrategy<T>, value: T, serializersModule: SerializersModule) =
    IndexedDataOutputEncoder(output, serializersModule).encodeSerializableValue(serializer, value)

@ExperimentalSerializationApi
inline fun <reified T: Any> encodeTo(output: DataOutput, value: T, serializersModule: SerializersModule) =
    encodeTo(output, serializer(), value, serializersModule)

@ExperimentalSerializationApi
fun <T> decodeFrom(input: DataInput, deserializer: DeserializationStrategy<T>, serializersModule: SerializersModule): T =
    DataInputDecoder(input = input, serializersModule = serializersModule).decodeSerializableValue(deserializer)

@ExperimentalSerializationApi
inline fun <reified T> decodeFrom(input: DataInput, serializersModule: SerializersModule): T =
    decodeFrom(input, serializer(), serializersModule)

@ExperimentalUnsignedTypes
fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').toUpperCase()}}"
}

fun <T> ArrayDeque<T>.prepend(value: T) { addFirst(value) }
fun <T> ArrayDeque<T>.prepend(list: List<T>) { addAll(0, list) }