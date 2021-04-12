package com.ing.serialization.bfl.serializers

import java.math.BigDecimal

fun String.toListOfDecimals() = map {
    Character.getNumericValue(it).toByte()
}.toByteArray()

fun BigDecimal.representOrThrow(): Pair<String, String?> {
    val integerFractionPair = toPlainString().removePrefix("-").split(".")

    val integerPart = integerFractionPair[0]
    val fractionalPart = integerFractionPair.getOrNull(1)

    return Pair(integerPart, fractionalPart)
}

/**
 * The encoding is as follows:
 * - sign: -1, 0 or 1
 * - integer: little-endian encoded ByteArray, so the number 123 is encoded as ByteArray(3): [3, 2, 1]
 * - fraction: big-endian encoded ByteArray, so the number 123 is encoced as ByteArray(3): [1, 2, 3]
 * @return a triple containing the sign byte, the integer bytes and the fraction bytes
 */
fun BigDecimal.asByteTriple(): Triple<Byte, ByteArray, ByteArray> {
    val (integerPart, fractionalPart) = representOrThrow()
    val sign = signum().toByte()
    val integer = integerPart.toListOfDecimals().reversedArray()
    val fraction = (fractionalPart?.toListOfDecimals() ?: ByteArray(0))
    return Triple(sign, integer, fraction)
}
