import annotations.DFLength
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.security.PublicKey

@Serializable
class Own(val int: Int = 100) {
    override fun toString() = "Own(int= $int)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Own

        if (int != other.int) return false

        return true
    }

    override fun hashCode(): Int {
        return int
    }


}

@Serializable
class OwnList(@DFLength([2]) val list: List<Int> = listOf(1)) {
    override fun toString() = "OwnList(list= ${list.joinToString()})"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OwnList

        if (list != other.list) return false

        return true
    }

    override fun hashCode(): Int {
        return list.hashCode()
    }
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