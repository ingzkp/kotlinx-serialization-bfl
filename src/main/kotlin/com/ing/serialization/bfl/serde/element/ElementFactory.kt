package com.ing.serialization.bfl.serde.element

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serde.SerdeError
import com.ing.serialization.bfl.serde.isCollection
import com.ing.serialization.bfl.serde.isContextual
import com.ing.serialization.bfl.serde.isPolymorphic
import com.ing.serialization.bfl.serde.isString
import com.ing.serialization.bfl.serde.isStructure
import com.ing.serialization.bfl.serde.isTrulyPrimitive
import com.ing.serialization.bfl.serde.prepend
import com.ing.serialization.bfl.serde.simpleSerialName
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.descriptors.getContextualDescriptor
import kotlinx.serialization.descriptors.getPolymorphicDescriptors
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

class ElementFactory(private val serializersModule: SerializersModule = EmptySerializersModule) {
    companion object {
        const val polySerialNameLength = 100
    }

    private var dfQueue = ArrayDeque<Int>()

    /**
     * Parses a class structure property by property recursively.
     * If withPropertyName is not null, this class structure is a property of some other wrapping structure.
     */
    fun parse(descriptor: SerialDescriptor, withPropertyName: String? = null): Element {
        val parentName = withPropertyName ?: descriptor.simpleSerialName

        return when {
            descriptor.isStructure -> {
                val children = (0 until descriptor.elementsCount)
                    .map { idx ->
                        val propertyName = descriptor.getElementName(idx)
                        val lengths = descriptor.getElementAnnotations(idx)
                            .filterIsInstance<FixedLength>()
                            .firstOrNull()?.lengths?.toList()?.let { ArrayDeque(it) }
                            ?: listOf()
                        dfQueue.prepend(lengths)

                        fromType(descriptor.getElementDescriptor(idx), "$parentName.$propertyName")
                    }
                StructureElement(descriptor.serialName, children, descriptor.isNullable)
            }
            descriptor.isPolymorphic -> fromType(descriptor, parentName)
            else -> error("${descriptor.serialName} is not supported")
        }
    }

    private fun fromType(descriptor: SerialDescriptor, parentName: String): Element {
        val name = descriptor.serialName

        return when {
            descriptor.isTrulyPrimitive -> PrimitiveElement(name, descriptor.kind, descriptor.isNullable)
            descriptor.isString -> {
                val requiredLength = dfQueue.removeFirstOrNull()
                    ?: throw SerdeError.InsufficientLengthData(descriptor, parentName)
                StringElement(name, requiredLength, descriptor.isNullable)
            }
            descriptor.isCollection -> {
                val requiredLength = dfQueue.removeFirstOrNull()
                    ?: throw SerdeError.InsufficientLengthData(descriptor, parentName)
                val children = descriptor.elementDescriptors.map { fromType(it, parentName) }
                CollectionElement(
                    name,
                    children,
                    CollectionSizingInfo(requiredLength = requiredLength),
                    descriptor.isNullable
                )
            }
            descriptor.isStructure -> {
                val isAnnotated = (0 until descriptor.elementsCount)
                    .any { idx -> descriptor.getElementAnnotations(idx).isNotEmpty() }

                if (isAnnotated) {
                    parse(descriptor, parentName)
                } else {
                    val children = descriptor.elementDescriptors.mapIndexed { idx, element ->
                        val propertyName = descriptor.getElementName(idx)
                        fromType(element, "$parentName.$propertyName")
                    }
                    StructureElement(name, children, descriptor.isNullable)
                }
            }
            descriptor.isPolymorphic -> {
                // Polymorphic type consists of a string and a structure.

                // Bound the serialName of the polymorphic type.
                val type = descriptor.elementDescriptors.first()
                dfQueue.prepend(polySerialNameLength)

                // Get the descriptor for the polymorphic type.
                val polyDescriptors = serializersModule.getPolymorphicDescriptors(descriptor)

                if (polyDescriptors.isEmpty()) {
                    throw SerdeError.NoPolymorphicSerializers(descriptor)
                }
                // TODO validate
                //  We work with fixed length serialization, thus
                //  we assume that all interfaces are serialized through the same surrogate or descriptor.
                //  thus any descriptor will do.
                val value = polyDescriptors.first()

                val children = listOf(type, value).map { fromType(it, parentName) }

                StructureElement(name, children, descriptor.isNullable)
            }
            descriptor.isContextual -> {
                val contextDescriptor = serializersModule.getContextualDescriptor(descriptor)
                    ?: throw SerdeError.NoContextualSerializer(descriptor)

                fromType(contextDescriptor, parentName)
            }
            else -> error("Do not know how to build element from type ${descriptor.serialName}")
        }
    }
}
