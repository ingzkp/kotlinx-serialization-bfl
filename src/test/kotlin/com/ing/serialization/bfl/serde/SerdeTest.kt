package com.ing.serialization.bfl.serde

import com.ing.serialization.bfl.deserialize
import com.ing.serialization.bfl.serialize
import com.ing.serialization.bfl.serializers.RSAPublicKeySerializer
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import sun.security.rsa.RSAPublicKeyImpl
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom

@ExperimentalSerializationApi
open class SerdeTest {
    companion object {
        val serializersModule = SerializersModule {
            polymorphic(PublicKey::class) {
                subclass(RSAPublicKeyImpl::class, RSAPublicKeySerializer)
            }
        }
        private val generator: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")

        init {
            generator.initialize(2048, SecureRandom())
        }
    }

    inline fun <reified T : Any> checkedSerialize(data: T, mask: List<Pair<String, Int>>): ByteArray {
        val bytes = serialize(data, serializersModule)
        log(bytes, mask)
        bytes.size shouldBe mask.sumBy { it.second }

        return bytes
    }

    inline fun <reified T : Any> serialize(data: T) = serialize(data, serializersModule)
    inline fun <reified T : Any> deserialize(bytes: ByteArray) = deserialize<T>(bytes, serializersModule)

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

    protected fun generatePublicKey(): PublicKey {
        return generator.genKeyPair().public
    }
}
