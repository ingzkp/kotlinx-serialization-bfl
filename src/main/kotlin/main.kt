import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*
import sun.security.rsa.RSAPublicKeyImpl
import java.io.*
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom
import kotlin.random.Random
import kotlin.reflect.KClass

class DataOutputEncoder(val output: DataOutput, val scheme: KClass<*>) : AbstractEncoder() {
    private val baSerializer = serializer<ByteArray>()

    override val serializersModule: SerializersModule = SerializersModule {
        polymorphic(PublicKey::class) {
            subclass(RSAPublicKeyImpl::class, RSAPublicKeySerializer)
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
    override fun encodeString(value: String) = output.writeUTF(value)
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = output.writeInt(index)

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        encodeInt(collectionSize)
        return this
    }

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        super.encodeSerializableValue(serializer, value)
    }

    override fun encodeNull() = encodeBoolean(false)
    override fun encodeNotNullMark() = encodeBoolean(true)
}

fun <T: Any> encodeTo(output: DataOutput, serializer: SerializationStrategy<T>, value: T) {
    val encoder = DataOutputEncoder(output, value::class)
    encoder.encodeSerializableValue(serializer, value)
}

inline fun <reified T: Any> encodeTo(output: DataOutput, value: T){
    val serializer = serializer<T>()
    encodeTo(output, serializer, value)
}

class DataInputDecoder(val input: DataInput, var elementsCount: Int = 0) : AbstractDecoder() {
    private val baSerializer = serializer<ByteArray>()

    private var elementIndex = 0

    override val serializersModule: SerializersModule = SerializersModule {
        polymorphic(PublicKey::class) {
            subclass(RSAPublicKeyImpl::class, RSAPublicKeySerializer)
        }
    }

    override fun decodeBoolean(): Boolean = input.readByte().toInt() != 0
    override fun decodeByte(): Byte = input.readByte()
    override fun decodeShort(): Short = input.readShort()
    override fun decodeInt(): Int = input.readInt()
    override fun decodeLong(): Long = input.readLong()
    override fun decodeFloat(): Float = input.readFloat()
    override fun decodeDouble(): Double = input.readDouble()
    override fun decodeChar(): Char = input.readChar()
    override fun decodeString(): String = input.readUTF()

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = input.readInt()

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == elementsCount) return CompositeDecoder.DECODE_DONE
        return elementIndex++
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        DataInputDecoder(input, descriptor.elementsCount)

    override fun decodeSequentially(): Boolean = true

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int =
        decodeInt().also { elementsCount = it }

    override fun decodeNotNullMark(): Boolean = decodeBoolean()
}

fun <T> decodeFrom(input: DataInput, deserializer: DeserializationStrategy<T>): T {
    val decoder = DataInputDecoder(input)
    return decoder.decodeSerializableValue(deserializer)
}

inline fun <reified T> decodeFrom(input: DataInput): T = decodeFrom(input, serializer())

//////////////////////
@Serializable
data class User(val id: PublicKey)

@Serializable
data class GenericUser<U>(val id: U)

inline fun <reified T> test(
    data: T,
    serde: KSerializer<T>? = null
) {
    println("Data:\n$data\n")
    //--------

    val output = ByteArrayOutputStream()

    if (serde != null) {
        encodeTo(DataOutputStream(output), serde, data!!)
    } else {
        encodeTo(DataOutputStream(output), data!!)
    }

    val bytes = output.toByteArray()
    println("Serialized:\n${bytes.joinToString(",")}\n")
    // --------

    val input = ByteArrayInputStream(bytes)

    val obj = if (serde != null) {
        decodeFrom<T>(DataInputStream(input), serde)
    } else {
        decodeFrom<T>(DataInputStream(input))
    }
    println("Deserialized:\n$obj\n")
}

fun main() {
    // Generate some public key
    // val pk = getRSA()

    // Simple inclusion of a public key
    // val u = User(pk)
    // test(u)

    // Inclusion of PublicKey as a generic
    // val gu = GenericUser(pk)
    // test(gu)

    testML()
}

fun getRSA(): PublicKey {
    val generator = KeyPairGenerator.getInstance("RSA")
    generator.initialize(2048, SecureRandom())
    return generator.genKeyPair().public
}

@Serializable
@SerialName("RSAPublicKeyImpl")
class RSAPublicKeySurrogate(val encoded: ByteArray)

object RSAPublicKeySerializer : KSerializer<RSAPublicKeyImpl> {
    override val descriptor = RSAPublicKeySurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: RSAPublicKeyImpl) {
        encoder.encodeSerializableValue(
            RSAPublicKeySurrogate.serializer(),
            RSAPublicKeySurrogate(value.encoded)
        )
    }

    override fun deserialize(decoder: Decoder): RSAPublicKeyImpl {
        val surrogate = decoder.decodeSerializableValue(RSAPublicKeySurrogate.serializer())
        return RSAPublicKeyImpl(surrogate.encoded)
    }
}


@Serializable
class ListSurrogate<T>(val list: List<T>, private val trash: ByteArray)

class ListSerializer<T>(val inner: KSerializer<T>) : KSerializer<List<T>> {
    val strategy = ListSurrogate.serializer(inner)
    override val descriptor: SerialDescriptor = strategy.descriptor

    @ExperimentalStdlibApi
    override fun serialize(encoder: Encoder, value: List<T>) {
        require(value.size <= 5) { "sad" }
        val a = inner.descriptor
        // T here is Int
        // assume total size is 5 * sizeOf(Int) = 20
        val surrogate = ListSurrogate(value, Random.nextBytes(20 - 5 * value.size))
        encoder.encodeSerializableValue(strategy, surrogate)
    }

    override fun deserialize(decoder: Decoder): List<T> {
        val surrogate = decoder.decodeSerializableValue(strategy)
        return surrogate.list
    }
}

annotation class Tag(val a: Int)

@Serializable
data class ML(
    @Serializable(with=ListSerializer::class)
    val list: List<User>
)

fun testML() {
    val data = ML(listOf(User(getRSA())))
    val output = ByteArrayOutputStream()

    encodeTo(DataOutputStream(output), data)

    val bytes = output.toByteArray()
    println("Serialized:\n${bytes.joinToString(",")}\n")
    // --------

    val input = ByteArrayInputStream(bytes)
    val obj = decodeFrom<ML>(DataInputStream(input))

    println("Deserialized:\n$obj\n")
}
