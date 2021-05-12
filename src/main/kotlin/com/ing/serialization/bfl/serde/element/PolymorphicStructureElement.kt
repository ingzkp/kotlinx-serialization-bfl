package com.ing.serialization.bfl.serde.element

import com.ing.serialization.bfl.serde.SerdeError
import com.ing.serialization.bfl.serde.resolvePolymorphicChild
import kotlinx.serialization.modules.SerializersModule
import java.io.DataInput
import java.io.DataOutput
import kotlin.reflect.KClass

class PolymorphicStructureElement(
    serialName: String,
    propertyName: String,
    inner: MutableList<Element>,
    isNullable: Boolean
) : StructureElement(serialName, propertyName, inner, isNullable) {
    var baseClass: KClass<in Any>? = null

    override fun encodeNull(output: DataOutput) {
        val type = inner.first().expect<StringElement>()
        val value = inner.last().expect<StructureElement>()
        type.encode(value.serialName, output)
        value.encodeNull(output)
    }

    fun decodeNull(input: DataInput, serializersModule: SerializersModule) {
        val type = inner.first().expect<StringElement>().decode(input)
        val placeholderValue = inner.last().expect<StructureElement>()
        val descriptor = serializersModule.getPolymorphic(
            baseClass = requireNotNull(baseClass) { "Something went wrong - Base class of polymorphic StructureElement should have been set" },
            serializedClassName = type
        )?.descriptor ?: throw SerdeError.NoPolymorphicSerializerForSubClass(type)
        val newValue = this.resolvePolymorphicChild(descriptor, placeholderValue.propertyName, serializersModule)
        newValue.decodeNull(input)
    }

    override fun verifySelfResolvabilityOrThrow() {
        // if a null polymorphic has not been resolved in the parsing stage, an exception is thrown
        isNull && throw SerdeError.NonResolvablePolymorphic(serialName)
    }

    override fun clone(): PolymorphicStructureElement = PolymorphicStructureElement(serialName, propertyName, inner, isNullable).also {
        it.isNull = isNull
    }
}
