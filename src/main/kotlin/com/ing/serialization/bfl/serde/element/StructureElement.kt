package com.ing.serialization.bfl.serde.element

import com.ing.serialization.bfl.serde.Element
import kotlinx.serialization.ExperimentalSerializationApi
import java.io.DataOutput

@ExperimentalSerializationApi
class StructureElement(name: String, val inner: List<Element>, override val isNullable: Boolean) : Element(name) {

    override val layout by lazy {
        val layout = listOf(Pair("length", constituentsSize))
        Layout(name, nullLayout + layout, inner.map { it.layout })
    }

    private val constituentsSize by lazy {
        inner.sumBy { it.size }
    }

    override fun encodeNull(stream: DataOutput) = repeat(constituentsSize) { stream.writeByte(0) }
}
