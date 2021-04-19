package com.ing.serialization.bfl.serde.element

import java.io.DataOutput

class StructureElement(
    serialName: String,
    propertyName: String,
    inner: List<Element>,
    override var isNullable: Boolean
) : Element(serialName, propertyName, inner) {
    override val inherentLayout by lazy {
        listOf(Pair("[Structure] length", constituentsSize))
    }

    private val constituentsSize by lazy {
        inner.sumBy { it.size }
    }

    override fun encodeNull(output: DataOutput) = repeat(constituentsSize) { output.writeByte(0) }
}
