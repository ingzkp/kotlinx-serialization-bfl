package com.ing.serialization.bfl.serde.element

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import java.io.DataInput
import java.io.DataOutput

class StructureElement(
    serialName: String,
    propertyName: String,
    inner: MutableList<Element>,
    override var isNullable: Boolean
) : Element(serialName, propertyName, inner) {
    override val inherentLayout by lazy {
        listOf(
            Pair("[Structure] length", constituentsSize),
        )
    }

    private val constituentsSize by lazy {
        inner.sumBy { it.size }
    }

    override fun encodeNull(output: DataOutput) {
        if (isPolymorphic) {
            val type = inner.first().expect<StringElement>()
            val value = inner.last().expect<StructureElement>()
            type.encode(value.serialName, output)
            value.encodeNull(output)
        } else {
            inner.forEach { it.encodeNull(output) }
        }
    }

    override fun decodeNull(input: DataInput) {
        inner.forEach { it.decodeNull(input) }
    }

    fun decodeNullPolymorphic(input: DataInput, serializersModule: SerializersModule) {
        val type = inner.first().expect<StringElement>().decode(input)
        val placeholderValue = inner.last().expect<StructureElement>()
        val descriptor = serializersModule.serializer(Class.forName(type)).descriptor
        val newValue = ElementFactory(serializersModule)
            .parse(descriptor, placeholderValue.propertyName)
            .expect<StructureElement>().also {
                it.parent = this
            }
        val ind = inner.indexOfFirst { element -> element is StructureElement }
        inner[ind] = newValue
        newValue.decodeNull(input)
    }

    fun assignParentToChildren() {
        inner.forEach { it.parent = this }
    }

    override fun clone(): StructureElement = StructureElement(serialName, propertyName, inner, isNullable).also {
        it.isPolymorphic = isPolymorphic
        it.isNull = isNull
        it.assignParentToChildren()
    }
}
