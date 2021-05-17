package com.ing.serialization.bfl.serde.serializers.custom.polymorphic

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import com.ing.serialization.bfl.api.reified.debugSerialize as debugSerializeInlined

class SealedClassTest {
    @Serializable
    data class Data(val value: @Polymorphic SecureHash)

    private val serializers = SerializersModule {
        polymorphic(SecureHash::class) {
            subclass(SecureHash.SHA256::class, SecureHashSHA256Serializer)
            subclass(SecureHash.HASH::class, SecureHashHASHSerializer)
        }
        contextual(SecureHashSerializer)
        contextual(SecureHashSHA256Serializer)
        contextual(SecureHashHASHSerializer)
    }

    @Test
    fun `Sealed types are directly serializable`() {
        val data = SecureHash.allOnesHash

        val serialization = debugSerializeInlined(data, serializersModule = serializers)
        println(serialization.second)
    }

    @Test
    fun `Sealed types as fields are serializable`() {
        val data = Data(SecureHash.allOnesHash)

        val serialization = debugSerializeInlined(data, serializersModule = serializers)
        println(serialization.second)
    }

    @Test
    fun `Sealed type should be the same after serialization and deserialization`() {
        val data = Data(SecureHash.allOnesHash)

        roundTripInlined(data, serializers)
        roundTrip(data, serializers)
    }

    @Test
    fun `different Sealed data objects should have same size after serialization`() {
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

object SecureHashSerializer :
    SurrogateSerializer<SecureHash, SecureHashSurrogate>(SecureHashSurrogate.serializer(), { SecureHashSurrogate.from(it) })

object SecureHashSHA256Serializer :
    SurrogateSerializer<SecureHash.SHA256, SecureHashSHA256Surrogate>(
        SecureHashSHA256Surrogate.serializer(), { SecureHashSHA256Surrogate(it.bytes) }
    )

object SecureHashHASHSerializer :
    SurrogateSerializer<SecureHash.HASH, SecureHashHASHSurrogate>(
        SecureHashHASHSurrogate.serializer(), { SecureHashHASHSurrogate(it.algorithm, it.bytes) }
    )

@Suppress("ArrayInDataClass")
@Serializable
@SerialName("SEC")
data class SecureHashSurrogate(
    @FixedLength([20])
    val algorithm: String,
    @FixedLength([32])
    val bytes: ByteArray
) : Surrogate<SecureHash> {
    override fun toOriginal() = when (algorithm) {
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

@Suppress("ArrayInDataClass")
@Serializable
@SerialName("256")
data class SecureHashSHA256Surrogate(
    @FixedLength([32])
    val bytes: ByteArray
) : Surrogate<SecureHash.SHA256> {
    override fun toOriginal() = SecureHash.SHA256(bytes)
}

@Suppress("ArrayInDataClass")
@Serializable
@SerialName("HSH")
data class SecureHashHASHSurrogate(
    @FixedLength([20])
    val algorithm: String,
    @FixedLength([32])
    val bytes: ByteArray
) : Surrogate<SecureHash.HASH> {
    override fun toOriginal() = SecureHash.HASH(algorithm, bytes)
}
