package com.ing.serialization.bfl.serde

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serialize
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom

@ExperimentalSerializationApi
inline fun <reified T : Any> checkedSerialize(
    data: T,
    mask: List<Pair<String, Int>>,
    serializersModule: SerializersModule = EmptySerializersModule
): ByteArray {
    val bytes = serialize(data, serializersModule)
    log(bytes, mask)
    bytes.size shouldBe mask.sumBy { it.second }

    return bytes
}

fun log(bytes: ByteArray, splitMask: List<Pair<String, Int>>) {
    println("Serialized:")
    // println("Raw: ${bytes.joinToString(separator = ",")}")
    var start = 0
    splitMask.forEach {
        val range = bytes.copyOfRange(start, start + it.second)
        val repr = range.joinToString(separator = ",") { d -> String.format("%2d", d) }
        println("${it.first} [$start, ${start + it.second}]\t: $repr")
        start += it.second
    }
}

fun generateRSAPubKey(): PublicKey {
    val generator: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
    generator.initialize(2048, SecureRandom())
    return generator.genKeyPair().public
}

@Serializable
data class Own(val int: Int = 100) {
    override fun toString() = "Own(int= $int)"
}

@ExperimentalSerializationApi
@Serializable
data class OwnList(@FixedLength([2]) val list: List<Int> = listOf(1)) {
    override fun toString() = "OwnList(list= ${list.joinToString()})"
}
