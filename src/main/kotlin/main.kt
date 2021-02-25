import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import sun.security.rsa.RSAPublicKeyImpl
import java.io.*
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

data class CollectionMeta(
    var start: Int?,
    var occupies: Int?,
    val annotations: List<Annotation>,
    val free: MutableMap<String, Any>
    )

@ExperimentalSerializationApi
class IndexedDataOutputEncoder(val output: DataOutput, val defaults: List<out Any>) : AbstractEncoder() {

    // we cannot guarantee whether stack or queue will be ok
    private val collections = mutableMapOf<Int, CollectionMeta>()

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
                        collections[child.hashCode()] = CollectionMeta(
                            start = null,
                            occupies = null,
                            descriptor.getElementAnnotations(idx),
                            mutableMapOf("field" to descriptor.getElementName(idx))
                        )
                    }
                    //
                    else -> {}
                }
            }

        return super.beginStructure(descriptor)
    }

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        encodeInt(collectionSize)
        collections[descriptor.hashCode()]?.start = output.getCurrentByteIdx()
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        when (descriptor.kind) {
            StructureKind.LIST -> with (collections[descriptor.hashCode()]) {
                // Not removing this metadata because it may be handy for treating lists of ... lists of stuff.
                this ?: error(" Something does't add up")

                occupies = finalizeCollection(descriptor, annotations, start ?: error ("Wait a minute. Hang on a second."))
                free["processed"] = true
            }

            else -> {}
        }

        super.endStructure(descriptor)
    }

    private fun finalizeCollection(descriptor: SerialDescriptor, annotations: List<Annotation>, startIdx: Int): Int {
        val expectedNumberOfElements = annotations
            .filterIsInstance<FixedLength>()
            .firstOrNull()?.value

        require(expectedNumberOfElements != null) {
            "Collection `${descriptor.serialName}` must have FixedLength annotation"
        }

        val expectedLength = expectedNumberOfElements * getElementSize(descriptor.elementDescriptors.single())

        val currentByteIdx = output.getCurrentByteIdx()
        val actualLength = currentByteIdx - startIdx
        require(expectedLength > actualLength) {
            "Serialized elements don't fit into their expected length"
        }

        repeat(expectedLength - actualLength) { encodeByte(0) }

        return expectedLength
    }

    private fun getElementSize(descriptor: SerialDescriptor): Int =
        // TODO have a better look here, is descriptor always decomposable in primitive types?
        //   no it is not, it can also be a list or map or a class.
        kotlin.runCatching {
            // todo send the string representation of type there somehow
            //  serialName can be overridden, otherwise it coincides with the fully-qualified name
            Size.of(descriptor.serialName, defaults)
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
                is PrimitiveKind.STRING -> throw IllegalStateException("Serialize char arrays")
                else -> descriptor.elementDescriptors.sumBy { getElementSize(it) }
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
    @FixedLength(3)
    val dates: List<@Serializable(with = DateSerializer::class) Date>,

    @FixedLength(2)
    val pairs: List<Pair<Int, Int>>,

    @Serializable(with = DateSerializer::class)
    val date: Date,

    @FixedLength(2)
    val own: List<Own>
)

@ExperimentalSerializationApi
fun testML() {
    val data = ML(
        listOf(),
        listOf(Pair(1, 2)),
        SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"),
        listOf()
    )
    val output = ByteArrayOutputStream()

    encodeTo(DataOutputStream(output), data, Own(), DateSurrogate(Long.MIN_VALUE))

    val bytes = output.toByteArray()
    println("Serialized:\n${bytes.joinToString(",")}\n")
    // --------

    // val input = ByteArrayInputStream(bytes)
    // val obj = decodeFrom<ML>(DataInputStream(input))
    //
    // println("Deserialized:\n$obj\n")
}
