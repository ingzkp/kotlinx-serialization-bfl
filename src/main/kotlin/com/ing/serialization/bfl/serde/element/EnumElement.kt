package com.ing.serialization.bfl.serde.element

import java.io.DataInput
import java.io.DataOutput

class EnumElement(
    serialName: String,
    propertyName: String,
    override val isNullable: Boolean
) : Element(serialName, propertyName) {

    override val inherentLayout by lazy {
        listOf(Pair("[Enum] value length", 4))
    }

    fun encode(index: Int, stream: DataOutput) {
        stream.writeInt(index)
    }

    override fun encodeNull(output: DataOutput) = encode(0, output)

    fun decode(stream: DataInput): Int = stream.readInt()
}
