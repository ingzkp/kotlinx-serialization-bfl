import annotations.DFLength
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.security.PublicKey

@Serializable
class Own(val a: Int = 100) {
    override fun toString() = "Owns(a= $a)"
}

@Serializable
data class User(val id: PublicKey)

@Serializable
data class GenericUser<U>(val id: U)

@ExperimentalSerializationApi
@Suppress("ArrayInDataClass")
@Serializable
@SerialName("RSAPublicKeyImpl")
data class RSAPublicKeySurrogate(@DFLength([500]) val encoded: ByteArray)