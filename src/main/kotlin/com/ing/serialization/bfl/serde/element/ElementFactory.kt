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
                StructureElement(descriptor.serialName, parentName, children, descriptor.isNullable)
            }
            descriptor.isPolymorphic -> fromType(descriptor, parentName)
            else -> error("${descriptor.serialName} is not supported")
        }
    }

    private fun fromType(descriptor: SerialDescriptor, parentName: String): Element {
        val serialName = descriptor.serialName

        return when {
            descriptor.isTrulyPrimitive -> PrimitiveElement(serialName, parentName, descriptor.kind, descriptor.isNullable)
            descriptor.isString -> {
                val requiredLength = dfQueue.removeFirstOrNull()
                    ?: throw SerdeError.InsufficientLengthData(descriptor, parentName)
                StringElement(serialName, parentName, requiredLength, descriptor.isNullable)
            }
            descriptor.isCollection -> {
                val requiredLength = dfQueue.removeFirstOrNull()
                    ?: throw SerdeError.InsufficientLengthData(descriptor, parentName)
                val children = descriptor.elementDescriptors.map { fromType(it, parentName) }
                CollectionElement(
                    serialName = serialName,
                    propertyName = parentName,
                    inner = children,
                    requiredLength = requiredLength,
                    isNullable = descriptor.isNullable
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
                    StructureElement(serialName, parentName, children, descriptor.isNullable)
                }
            }
            descriptor.isPolymorphic -> {
                // Get the descriptor for the polymorphic type.
                val polyDescriptors = serializersModule.getPolymorphicDescriptors(descriptor)

                if (polyDescriptors.isEmpty()) {
                    throw SerdeError.NoPolymorphicSerializers(descriptor)
                }

                // To ensure fixed length serialization for a polymorphic type,
                // all variants of the polymorphic must have the same serialization size.
                // A robust way to achieve that is to use the same serializable surrogate class.
                // **We accept this as a hard requirement!**

                // TODO.
                //  In principle it is possible to validate that all surrogates are derived
                //  from the same base class and even parse th base class together with
                //  the respective annotations.
                polyDescriptors.forEach {
                    val clazz = Class.forName(it.serialName)
                    println(clazz.canonicalName)
                }

                // Polymorphic type consists of a string describing type and a structure.
                // Bound the serialName of the polymorphic type.
                val type = descriptor.elementDescriptors.first()
                dfQueue.prepend(polySerialNameLength)

                // The hard requirement above implies that any descriptor independently of a variant
                // will good enough for a respective polymorphic type.
                val value = polyDescriptors.first()

                val children = listOf(type, value).map { fromType(it, parentName) }

                StructureElement(serialName, parentName, children, descriptor.isNullable)
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
