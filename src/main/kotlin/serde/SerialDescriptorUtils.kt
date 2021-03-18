package serde

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind

@ExperimentalSerializationApi
val SerialDescriptor.isCollection: Boolean
    get() = kind is StructureKind.LIST || kind is StructureKind.MAP

@ExperimentalSerializationApi
val SerialDescriptor.isString: Boolean
    get() = kind is PrimitiveKind.STRING

@ExperimentalSerializationApi
val SerialDescriptor.isTrulyPrimitive: Boolean
    get() = kind is PrimitiveKind && kind !is PrimitiveKind.STRING

@ExperimentalSerializationApi
val SerialDescriptor.isStructure: Boolean
    get() = kind is StructureKind.CLASS

@ExperimentalSerializationApi
val SerialDescriptor.isPolymorphic: Boolean
    get() = kind is PolymorphicKind
