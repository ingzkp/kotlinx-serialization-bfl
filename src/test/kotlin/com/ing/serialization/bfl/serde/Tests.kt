package com.ing.serialization.bfl.serde

import com.ing.serialization.bfl.api.deserialize
import com.ing.serialization.bfl.api.serialize
import com.ing.serialization.bfl.serializers.BFLSerializers
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import com.ing.serialization.bfl.api.reified.deserialize as deserializeInlined
import com.ing.serialization.bfl.api.reified.serialize as serializeInlined

/**
 * Serialize and check whether serialization fits into the expected mask.
 */
inline fun <reified T : Any> checkedSerializeInlined(
    data: T,
    mask: List<Pair<String, Int>>,
    serializers: SerializersModule = EmptySerializersModule,
    strategy: KSerializer<T>? = null
): ByteArray {
    val bytes = serializeInlined(data, strategy, serializersModule = BFLSerializers + serializers)
    log(bytes, mask)
    bytes.size shouldBe mask.sumBy { it.second }

    return bytes
}

/**
 * Test if value survives serialization/deserialization.
 */
inline fun <reified T : Any> roundTripInlined(
    value: T,
    serializers: SerializersModule = EmptySerializersModule,
    strategy: KSerializer<T>? = null
) {
    val serialization = serializeInlined(value, strategy, serializersModule = BFLSerializers + serializers)
    val deserialization = deserializeInlined<T>(serialization, BFLSerializers + serializers)

    deserialization shouldBe value
}

/**
 * Test if serializations of different instances have the same size.
 */
inline fun <reified T : Any> sameSizeInlined(
    value1: T,
    value2: T,
    serializers: SerializersModule = EmptySerializersModule,
    strategy: KSerializer<T>? = null
) {
    value1 shouldNotBe value2

    val serialization1 = serializeInlined(value1, strategy, serializersModule = BFLSerializers + serializers)
    val serialization2 = serializeInlined(value2, strategy, serializersModule = BFLSerializers + serializers)

    serialization1 shouldNotBe serialization2
    serialization1.size shouldBe serialization2.size
}

/**
 * Serialize and check whether serialization fits into the expected mask.
 */
fun <T : Any> checkedSerialize(
    data: T,
    mask: List<Pair<String, Int>>,
    serializers: SerializersModule = EmptySerializersModule,
    strategy: KSerializer<T>? = null
): ByteArray {
    val bytes = serialize(data, strategy, serializersModule = BFLSerializers + serializers)
    log(bytes, mask)
    bytes.size shouldBe mask.sumBy { it.second }

    return bytes
}

/**
 * Test if value survives serialization/deserialization.
 */
fun <T : Any> roundTrip(value: T, klass: KClass<out T> = value::class, serializers: SerializersModule = EmptySerializersModule) {
    val serialization = serialize(value, serializersModule = BFLSerializers + serializers)
    val deserialization = deserialize(serialization, klass, BFLSerializers + serializers)

    deserialization shouldBe value
}

/**
 * Test if serializations of different instances have the same size.
 */
fun <T : Any> sameSize(
    value1: T,
    value2: T,
    serializers: SerializersModule = EmptySerializersModule,
    strategy: KSerializer<T>? = null
) {
    value1 shouldNotBe value2

    val serialization1 = serialize(value1, strategy, serializersModule = BFLSerializers + serializers)
    val serialization2 = serialize(value2, strategy, serializersModule = BFLSerializers + serializers)

    serialization1 shouldNotBe serialization2
    serialization1.size shouldBe serialization2.size
}
