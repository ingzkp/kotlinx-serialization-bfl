package com.ing.serialization.bfl.serializers

import java.math.BigDecimal

fun String.toListOfDecimals(): ByteArray {
    return this.map {
        // Experimental: prefer plain java version.
        // it.digitToInt()
        Character.getNumericValue(it).toByte()
    }.toByteArray()
}

fun BigDecimal.representOrThrow(): Pair<String, String?> {
    val integerFractionPair = toPlainString().removePrefix("-").split(".")

    val integerPart = integerFractionPair.getOrNull(0)
        ?: error("Cannot convert BigDecimal ${toPlainString()} to its integer and fractional parts")
    val fractionalPart = integerFractionPair.getOrNull(1)

    return Pair(integerPart, fractionalPart)
}

/**
 * @return a triple containing the sign byte, the integer bytes and the fraction bytes
 */
fun BigDecimal.asByteTriple(): Triple<Byte, ByteArray, ByteArray> {
    val (integerPart, fractionalPart) = representOrThrow()
    val sign = signum().toByte()
    val integer = integerPart.toListOfDecimals()
    val fraction = (fractionalPart?.toListOfDecimals() ?: ByteArray(0))
    return Triple(sign, integer, fraction)
}
