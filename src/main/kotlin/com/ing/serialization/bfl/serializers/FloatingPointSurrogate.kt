package com.ing.serialization.bfl.serializers

import com.ing.serialization.bfl.api.Surrogate
import java.math.BigDecimal

interface FloatingPointSurrogate<T> : Surrogate<T> {
    val sign: Byte
    val integer: ByteArray
    val fraction: ByteArray

    fun toBigDecimal(): BigDecimal {
        val integer = this.integer.reversedArray().joinToString(separator = "") { "$it" }
        val fraction = this.fraction.joinToString(separator = "") { "$it" }
        var digit = if (fraction.isEmpty()) integer else "$integer.$fraction"
        if (this.sign == (-1).toByte()) {
            digit = "-$digit"
        }
        return BigDecimal(digit)
    }
}
