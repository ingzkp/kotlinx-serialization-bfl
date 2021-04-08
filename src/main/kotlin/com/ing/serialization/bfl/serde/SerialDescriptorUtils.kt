package com.ing.serialization.bfl.serde

import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind

val SerialDescriptor.isCollection: Boolean
    get() = kind is StructureKind.LIST || kind is StructureKind.MAP

val SerialDescriptor.isString: Boolean
    get() = kind is PrimitiveKind.STRING

val SerialDescriptor.isTrulyPrimitive: Boolean
    get() = kind.isTrulyPrimitive

val SerialDescriptor.isStructure: Boolean
    get() = kind is StructureKind.CLASS

val SerialDescriptor.isPolymorphic: Boolean
    get() = kind is PolymorphicKind

val SerialDescriptor.isContextual: Boolean
    get() = kind is SerialKind.CONTEXTUAL

val SerialDescriptor.simpleSerialName: String
    get() = serialName.split(".").last()

val SerialKind.isTrulyPrimitive: Boolean
    get() = this is PrimitiveKind &&
        this !is PrimitiveKind.STRING
