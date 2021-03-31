package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.deserialize
import com.ing.serialization.bfl.serialize
import com.ing.serialization.bfl.serializeX
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class ContextualTypeTest {
    @Serializable
    data class Data(val value: @Contextual SecureHash)

    private val serializers = SerializersModule {
        contextual(SecureHashSerializer)
    }

    @Test
    fun `contextual types are serializable`() {
        val data = Data(SecureHash.allOnesHash)

        val serialization = serializeX(data, serializers)
        println(serialization.second)
    }

    @Test
    fun `serialize and deserialize contextual type`() {
        val data = Data(SecureHash.allOnesHash)

        val serialization = serialize(data, serializers)
        val deserialization = deserialize<Data>(serialization, serializers)

        assert(deserialization == data) { "Deserializion must coincide with the original data" }
    }

    @Test
    fun `serialization has fixed length`() {
        val data1 = Data(SecureHash.allOnesHash)
        val data2 = Data(SecureHash.zeroHash)
        serialize(data1, serializers).size shouldBe serialize(data2, serializers).size
    }
}

/**
 * This is a poor imitation of SecureHas class in Corda.
 */
sealed class SecureHash(val bytes: ByteArray) {
    init {
        require(bytes.size == 32) { "Hash may be at most 32 bytes long" }
    }
    class SHA256(bytes: ByteArray) : SecureHash(bytes) {
        override fun equals(other: Any?) = (other is SHA256) && (bytes contentEquals other.bytes)
        override fun hashCode() = ByteBuffer.wrap(bytes).int
    }
    class HASH(val algorithm: String, bytes: ByteArray) : SecureHash(bytes) {
        override fun equals(other: Any?) =
            (other is HASH) && (algorithm == other.algorithm) && (bytes contentEquals other.bytes)
        override fun hashCode() = ByteBuffer.wrap(bytes).int
    }

    companion object {
        val allOnesHash = SHA256(ByteArray(32) { 255.toByte() })
        val zeroHash: SHA256 = SHA256(ByteArray(32) { 0.toByte() })
    }
}

object SecureHashSerializer : KSerializer<SecureHash> {
    private val strategy = SecureHashSurrogate.serializer()
    override val descriptor: SerialDescriptor = strategy.descriptor

    override fun deserialize(decoder: Decoder): SecureHash {
        return decoder.decodeSerializableValue(strategy).toOriginal()
    }

    override fun serialize(encoder: Encoder, value: SecureHash) {
        encoder.encodeSerializableValue(strategy, SecureHashSurrogate.from(value))
    }
}

@Suppress("ArrayInDataClass")

@Serializable
data class SecureHashSurrogate(
    @FixedLength([20])
    val algorithm: String,
    @FixedLength([32])
    val bytes: ByteArray
) {
    fun toOriginal() = when (algorithm) {
        SHA256_algo -> SecureHash.SHA256(bytes)
        else -> SecureHash.HASH(algorithm, bytes)
    }

    companion object {
        const val SHA256_algo = "SHA256"
        fun from(original: SecureHash): SecureHashSurrogate {
            val algorithm = when (original) {
                is SecureHash.SHA256 -> SHA256_algo
                is SecureHash.HASH -> original.algorithm
            }

            return SecureHashSurrogate(algorithm, original.bytes)
        }
    }
}
