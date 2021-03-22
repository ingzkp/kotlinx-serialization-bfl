import com.ing.serialization.bfl.serde.BinaryFixedLengthInputDecoder
import com.ing.serialization.bfl.serde.BinaryFixedLengthOutputEncoder
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInput
import java.io.DataInputStream
import java.io.DataOutput
import java.io.DataOutputStream

@ExperimentalSerializationApi
fun <T : Any> encodeTo(
    output: DataOutput,
    serializer: SerializationStrategy<T>,
    value: T,
    serializersModule: SerializersModule
) =
    BinaryFixedLengthOutputEncoder(output, serializersModule).encodeSerializableValue(serializer, value)

@ExperimentalSerializationApi
inline fun <reified T : Any> serialize(data: T, serializersModule: SerializersModule): ByteArray {
    val output = ByteArrayOutputStream()
    val stream = DataOutputStream(output)
    encodeTo(stream, serializer(), data, serializersModule)
    return output.toByteArray()
}

@ExperimentalSerializationApi
fun <T> decodeFrom(
    input: DataInput,
    deserializer: DeserializationStrategy<T>,
    serializersModule: SerializersModule
): T =
    BinaryFixedLengthInputDecoder(input = input, serializersModule = serializersModule).decodeSerializableValue(
        deserializer
    )

@ExperimentalSerializationApi
inline fun <reified T : Any> deserialize(data: ByteArray, serializersModule: SerializersModule): T {
    val input = ByteArrayInputStream(data)
    val stream = DataInputStream(input)
    return decodeFrom(stream, serializer(), serializersModule)
}

fun <T> ArrayDeque<T>.prepend(value: T) {
    addFirst(value)
}

fun <T> ArrayDeque<T>.prepend(list: List<T>) {
    addAll(0, list)
}
