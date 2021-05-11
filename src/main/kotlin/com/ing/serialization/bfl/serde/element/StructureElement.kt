package com.ing.serialization.bfl.serde.element

import com.ing.serialization.bfl.serde.SerdeError
import com.ing.serialization.bfl.serde.resolvePolymorphicChild
import kotlinx.serialization.modules.SerializersModule
import java.io.DataInput
import java.io.DataOutput
import kotlin.reflect.KClass

class StructureElement(
    serialName: String,
    propertyName: String,
    inner: MutableList<Element>,
    override var isNullable: Boolean
) : Element(serialName, propertyName, inner) {
    init {
        inner.forEach { it.parent = this }
    }
    // set only in polymorphic StructureElements
    var baseClass: KClass<in Any>? = null

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
        val descriptor = serializersModule.getPolymorphic(
            baseClass = requireNotNull(baseClass) { "Something went wrong - Base class of polymorphic StructureElement should have been set" },
            serializedClassName = type
        )?.descriptor ?: throw SerdeError.NoPolymorphicSerializerForSubClass(type)
        val newValue = this.resolvePolymorphicChild(descriptor, placeholderValue.propertyName, serializersModule)
        newValue.decodeNull(input)
    }

    override fun clone(): StructureElement = StructureElement(serialName, propertyName, inner, isNullable).also {
        it.isPolymorphic = isPolymorphic
        it.isNull = isNull
    }
}
