package com.ing.serialization.bfl.serde

import com.ing.serialization.bfl.serde.element.Layout
import kotlinx.serialization.ExperimentalSerializationApi
import java.io.DataInput
import java.io.DataOutput

/**
 * The basic abstraction of each object being serialized.
 */
@ExperimentalSerializationApi
class StringElement(name: String, val requiredLength: Int, override val isNullable: Boolean) : Element(name) {
    override val layout by lazy {
        // BOOLEAN (if nullable; value present -> 1) + SHORT (string length) + requiredLength * length(CHAR)

        val layout = listOf(
            Pair("length", 2),
            Pair("value", requiredLength * 2)
        )

        Layout(name, nullLayout + layout, listOf())
    }

    /**
     * Returns the number of bytes the string to be padded.
     *
     * @throws SerdeError.StringTooLarge exception when string doesn't fit its given limit
     */
    private fun padding(actualLength: Int): Int {
        if (requiredLength < actualLength)
            throw SerdeError.StringTooLarge(actualLength, this)

        return 2 * (requiredLength - actualLength)
    }

    fun encode(string: String?, stream: DataOutput) {
        val actualLength = string?.length ?: 0

        // In output.writeUTF, length of the string is stored as short.
        // We do the same for consistency.
        stream.writeShort(actualLength)
        string?.forEach { stream.writeChar(it.toInt()) }
        repeat(padding(actualLength)) { stream.writeByte(0) }
    }

    override fun encodeNull(stream: DataOutput) = encode(null, stream)

    fun decode(stream: DataInput): String {
        // In output.writeUTF, length of the string is stored as short.
        // We do the same for consistency.
        val actualLength = stream.readShort().toInt()
        val string = (0 until actualLength).map { stream.readChar() }.joinToString("")

        stream.skipBytes(padding(actualLength))

        return string
    }
}
