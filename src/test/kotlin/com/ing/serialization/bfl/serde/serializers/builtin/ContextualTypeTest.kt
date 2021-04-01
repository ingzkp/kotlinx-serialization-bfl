package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
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
import com.ing.serialization.bfl.api.reified.debugSerialize as debugSerializeInlined

class ContextualTypeTest {
    @Serializable
    data class Data(val value: @Contextual SecureHash)

    private val serializers = SerializersModule {
        contextual(SecureHashSerializer)
        contextual(SecureHashSHA256Serializer)
        contextual(SecureHashHASHSerializer)
    }

    @Test
    fun `contextual types are directly serializable`() {
        val data = SecureHash.allOnesHash

        val serialization = debugSerializeInlined(data, serializers)
        println(serialization.second)
    }

    @Test
    fun `contextual types as fields are serializable`() {
        val data = Data(SecureHash.allOnesHash)

        val serialization = debugSerializeInlined(data, serializers)
        println(serialization.second)
    }

    @Test
    fun `serialize and deserialize contextual type`() {
        val data = Data(SecureHash.allOnesHash)

        roundTripInlined(data, serializers)
        roundTrip(data, data::class, serializers)
    }

    @Test
    fun `serialization has fixed length`() {
        val data1 = Data(SecureHash.allOnesHash)
        val data2 = Data(SecureHash.zeroHash)

        sameSizeInlined(data2, data1, serializers)
        sameSize(data2, data1, serializers)
    }
}

/**
 * This is a poor imitation of SecureHash class in Corda.
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

open class SealedSecureHashSerializer<T : SecureHash> : KSerializer<T> {
    private val strategy = SecureHashSurrogate.serializer()
    override val descriptor: SerialDescriptor = strategy.descriptor

    override fun deserialize(decoder: Decoder): T {
        @Suppress("UNCHECKED_CAST")
        return decoder.decodeSerializableValue(strategy).toOriginal() as? T
            ?: error("Cannot deserialize SecureHash")
    }

    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeSerializableValue(strategy, SecureHashSurrogate.from(value))
    }
}

object SecureHashSerializer : KSerializer<SecureHash> by SealedSecureHashSerializer()
object SecureHashSHA256Serializer : KSerializer<SecureHash.SHA256> by SealedSecureHashSerializer()
object SecureHashHASHSerializer : KSerializer<SecureHash.HASH> by SealedSecureHashSerializer()

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
