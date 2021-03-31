package com.ing.serialization.bfl.serde

import com.ing.serialization.bfl.deserialize
import com.ing.serialization.bfl.serialize
import com.ing.serialization.bfl.serializers.BFLSerializers
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Serialize and check whether serialization fits into the expected mask.
 */

inline fun <reified T : Any> checkedSerialize(
    data: T,
    mask: List<Pair<String, Int>>
): ByteArray {
    val bytes = serialize(data, BFLSerializers)
    log(bytes, mask)
    bytes.size shouldBe mask.sumBy { it.second }

    return bytes
}

/**
 * Test if value survives serialization/deserialization.
 */

inline fun <reified T : Any> roundTrip(value: T) {
    val serialization = serialize(value, BFLSerializers)
    val deserialization = deserialize<T>(serialization, BFLSerializers)

    deserialization shouldBe value
}

/**
 * Test if serializations of different instances have the same size.
 */

inline fun <reified T : Any> sameSize(value1: T, value2: T) {
    value1 shouldNotBe value2

    val serialization1 = serialize(value1, BFLSerializers)
    val serialization2 = serialize(value2, BFLSerializers)

    serialization1 shouldNotBe serialization2
    serialization1.size shouldBe serialization2.size
}
