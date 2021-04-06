package com.ing.serialization.bfl.serde

import com.ing.serialization.bfl.annotations.FixedLength
import kotlinx.serialization.Serializable
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom

fun log(bytes: ByteArray, splitMask: List<Pair<String, Int>>) {
    println("Serialized [${bytes.size}]:")
    // println("Raw: ${bytes.joinToString(separator = ",")}")
    var start = 0
    splitMask.forEach {
        val range = bytes.copyOfRange(start, start + it.second)
        val repr = range.joinToString(separator = ",") { d -> " $d" }
        println("${it.first} [$start, ${start + it.second}]\t:$repr")
        start += it.second
    }
}

fun generateRSAPubKey(): PublicKey {
    val generator: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
    generator.initialize(2048, SecureRandom())
    return generator.genKeyPair().public
}

fun generateDSAPubKey(): PublicKey {
    val generator: KeyPairGenerator = KeyPairGenerator.getInstance("DSA")
    generator.initialize(2048, SecureRandom())
    return generator.genKeyPair().public
}

@Serializable
data class Own(val int: Int = 100) {
    override fun toString() = "Own(int= $int)"
}

@Serializable
data class OwnList(@FixedLength([2]) val list: List<Int> = listOf(1)) {
    override fun toString() = "OwnList(list= ${list.joinToString()})"
}
