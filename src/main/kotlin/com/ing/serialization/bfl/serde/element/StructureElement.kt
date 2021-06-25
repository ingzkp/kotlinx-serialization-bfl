package com.ing.serialization.bfl.serde.element

import java.io.DataInput
import java.io.DataOutput

open class StructureElement(
    serialName: String,
    propertyName: String,
    inner: MutableList<Element>,
    override var isNullable: Boolean
) : Element(serialName, propertyName, inner) {
    init {
        inner.forEach { it.parent = this }
    }

    override val inherentLayout by lazy {
        listOf(
            Pair("[Structure] length", constituentsSize),
        )
    }

    private val constituentsSize by lazy {
        inner.sumBy { it.size }
    }

    override fun encodeNull(output: DataOutput) {
        inner.forEach {
            if (it.isNullable) output.writeBoolean(false)
            it.encodeNull(output)
        }
    }

    override fun decodeNull(input: DataInput) {
        inner.forEach {
            if (it.isNullable) input.readBoolean()
            it.decodeNull(input)
        }
    }

    override fun clone(): StructureElement = StructureElement(serialName, propertyName, inner, isNullable).also {
        it.isNull = isNull
    }
}
