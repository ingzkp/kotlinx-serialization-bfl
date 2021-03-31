package com.ing.serialization.bfl.serde.element

import java.io.DataOutput

class StructureElement(name: String, inner: List<Element>, override val isNullable: Boolean) : Element(name, inner) {
    override val inherentLayout by lazy {
        listOf(Pair("length", constituentsSize))
    }

    private val constituentsSize by lazy {
        inner.sumBy { it.size }
    }

    override fun encodeNull(output: DataOutput) = repeat(constituentsSize) { output.writeByte(0) }
}
