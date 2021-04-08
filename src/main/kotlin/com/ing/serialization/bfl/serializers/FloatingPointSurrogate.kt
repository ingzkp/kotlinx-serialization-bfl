package com.ing.serialization.bfl.serializers

import java.math.BigDecimal

interface FloatingPointSurrogate {
    val sign: Byte
    val integer: ByteArray
    val fraction: ByteArray

    fun toBigDecimal(): BigDecimal {
        val integer = this.integer.joinToString(separator = "") { "$it" }.trimStart('0')
        val fraction = this.fraction.joinToString(separator = "") { "$it" }.trimEnd('0')
        var digit = if (fraction.isEmpty()) integer else "$integer.$fraction"
        if (this.sign == (-1).toByte()) {
            digit = "-$digit"
        }
        return BigDecimal(digit)
    }
}
