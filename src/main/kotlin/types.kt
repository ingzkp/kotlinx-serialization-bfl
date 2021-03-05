import annotations.DFLength
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.security.PublicKey

@Serializable
class Own(val int: Int = 100) {
    override fun toString() = "Own(int= $int)"
}

@Serializable
class OwnList(@DFLength([2]) val list: List<Int> = listOf(1)) {
    override fun toString() = "OwnList(list= ${list.joinToString()})"
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