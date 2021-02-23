
import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import sun.security.rsa.RSAPublicKeyImpl
import java.io.*
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import kotlin.random.Random
import kotlin.reflect.KClass

@ExperimentalSerializationApi
class IndexedDataOutputEncoder(val output: DataOutput, val defaults: List<out Any>) : AbstractEncoder() {

    private val propertyAnnotationsStack = Stack<List<Annotation>>()
    private val startingIdxStack = Stack<Int>()

    override val serializersModule: SerializersModule = SerializersModule {
        polymorphic(PublicKey::class) {
            subclass(RSAPublicKeyImpl::class, RSAPublicKeySerializer)
        }
    }

    init {
        propertyAnnotationsStack.push(emptyList())
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        descriptor.elementNames.toList()
            .indices
            .reversed()
            .forEach {
                propertyAnnotationsStack.push(descriptor.getElementAnnotations(it))
            }

        startingIdxStack.push(output.getCurrentByteIdx())

        return super.beginStructure(descriptor)
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        val expectedNumberOfElements = propertyAnnotationsStack.pop()
            .filterIsInstance<FixedLength>()
            .firstOrNull()?.value

        if (descriptor.kind == StructureKind.LIST) {
            require(expectedNumberOfElements != null) {
                "List-like structures `${descriptor.serialName}` must have FixedLength annotation"
            }

            // todo in fact there can be several elements, say Pair<A, B>
            val elementDescriptor = descriptor.getElementDescriptor(0)
            val expectedLength = expectedNumberOfElements * getElementSize(elementDescriptor)

            val currentByteIdx = output.getCurrentByteIdx()
            val startingByteIdx = startingIdxStack.pop()
            val actualLength = currentByteIdx - startingByteIdx
            require(expectedLength > actualLength) {
                "Serialized elements don't fit into their expected length"
            }

            repeat(expectedLength - actualLength) {
                encodeByte(0)
            }
        }

        super.endStructure(descriptor)
    }

    private fun getElementSize(descriptor: SerialDescriptor): Int  =
        when (descriptor.kind) {
            is PrimitiveKind.BOOLEAN -> 1
            is PrimitiveKind.BYTE -> 1
            is PrimitiveKind.SHORT -> 2
            is PrimitiveKind.INT -> 4
            is PrimitiveKind.LONG -> 8
            is PrimitiveKind.FLOAT -> throw IllegalStateException("Floats are not yet supported")
            is PrimitiveKind.DOUBLE -> throw IllegalStateException("Double are not yet supported")
            is PrimitiveKind.CHAR -> 2
            is PrimitiveKind.STRING -> throw IllegalStateException("Serialize char arrays")
            else -> {
                descriptor.serialName
                // todo send the string representation of type there somehow
                //  serialName can be overridden, otherwise it coincides with the fully-qualified name
                Size.of(descriptor.serialName, defaults)
            }
    }

    override fun encodeBoolean(value: Boolean) {
        output.writeByte(if (value) 1 else 0)
    }

    override fun encodeByte(value: Byte) {
        output.writeByte(value.toInt())
    }

    override fun encodeShort(value: Short) {
        output.writeShort(value.toInt())
    }

    override fun encodeInt(value: Int) {
        output.writeInt(value)
    }

    override fun encodeLong(value: Long) {
        output.writeLong(value)
    }

    override fun encodeFloat(value: Float) {
        output.writeFloat(value)
    }

    override fun encodeDouble(value: Double) {
        output.writeDouble(value)
    }

    override fun encodeChar(value: Char) {
        output.writeChar(value.toInt())
    }

    override fun encodeString(value: String) {
        encodeInt(value.length)
        value.forEach { this.encodeChar(it) }
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        output.writeInt(index)
    }

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        encodeInt(collectionSize)
        return this
    }

    override fun encodeNull() = encodeBoolean(false)
    override fun encodeNotNullMark() = encodeBoolean(true)

    private fun DataOutput.getCurrentByteIdx(): Int = (this as DataOutputStream).size()
}

@ExperimentalSerializationApi
fun <T: Any> encodeTo(output: DataOutput, serializer: SerializationStrategy<T>, value: T, defaults: List<out Any> = listOf()) {
    val encoder = IndexedDataOutputEncoder(output, defaults)
    encoder.encodeSerializableValue(serializer, value)
}

@ExperimentalSerializationApi
inline fun <reified T: Any> encodeTo(output: DataOutput, value: T, vararg defaults: Any){
    val serializer = serializer<T>()
    encodeTo(output, serializer, value, defaults.toList())
}

@ExperimentalSerializationApi
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

@ExperimentalSerializationApi
fun <T> decodeFrom(input: DataInput, deserializer: DeserializationStrategy<T>): T {
    val decoder = DataInputDecoder(input)
    return decoder.decodeSerializableValue(deserializer)
}

@ExperimentalSerializationApi
inline fun <reified T> decodeFrom(input: DataInput): T = decodeFrom(input, serializer())

//////////////////////
@Serializable
data class User(val id: PublicKey)

@Serializable
data class GenericUser<U>(val id: U)

@ExperimentalSerializationApi
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

@ExperimentalSerializationApi
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

@Suppress("ArrayInDataClass")
@Serializable
@SerialName("RSAPublicKeyImpl")
data class RSAPublicKeySurrogate(
    @FixedLength(500) val encoded: ByteArray,
    @FixedLength(20) val string: String
)

@SerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class FixedLength(val value: Int)

object RSAPublicKeySerializer : KSerializer<RSAPublicKeyImpl> {
    override val descriptor = RSAPublicKeySurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: RSAPublicKeyImpl) {
        encoder.encodeSerializableValue(
            RSAPublicKeySurrogate.serializer(),
            RSAPublicKeySurrogate(value.encoded, "foo.bar")
        )
    }

    override fun deserialize(decoder: Decoder): RSAPublicKeyImpl {
        val surrogate = decoder.decodeSerializableValue(RSAPublicKeySurrogate.serializer())
        return RSAPublicKeyImpl(surrogate.encoded) as RSAPublicKeyImpl
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

@Serializable
data class ML(
    @FixedLength(2)
    val dates: List<@Serializable(with = DateSerializer::class) Date>




    // // Problematic
    // @Serializable(with = DateSerializer::class)
    // val date: Date
    // @FixedLength(2)
    // val own: List<Own>
)

@ExperimentalSerializationApi
fun testML() {
    val data = ML(
        listOf()
    )
    val output = ByteArrayOutputStream()

    encodeTo(DataOutputStream(output), data, Own(), DateSurrogate(Long.MIN_VALUE))

    val bytes = output.toByteArray()
    println("Serialized:\n${bytes.joinToString(",")}\n")
    // --------

    val input = ByteArrayInputStream(bytes)
    val obj = decodeFrom<ML>(DataInputStream(input))

    println("Deserialized:\n$obj\n")
}
