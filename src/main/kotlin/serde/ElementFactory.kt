package serde

import annotations.FixedLength
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.descriptors.getPolymorphicDescriptors
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import prepend

@ExperimentalSerializationApi
class ElementFactory(private val serializersModule: SerializersModule = EmptySerializersModule) {
    companion object {
        const val polySerialNameLength = 100
    }
    private var dfQueue = ArrayDeque<Int>()

    fun parse(descriptor: SerialDescriptor): Element {
        return when {
                descriptor.isStructure -> {
                    val children = (0 until descriptor.elementsCount )
                        .map { idx ->
                            val lengths = descriptor.getElementAnnotations(idx)
                                .filterIsInstance<FixedLength>()
                                .firstOrNull()?.lengths?.toList()?.let { ArrayDeque(it) }
                                ?: listOf()
                            dfQueue.prepend(lengths)

                            try {
                                fromType("[${descriptor.serialName}]", descriptor.getElementDescriptor(idx))
                            } catch (err: SerdeError.InsufficientLengthData) {
                                throw SerdeError.CannotParse(
                                    "Property ${descriptor.serialName}.${descriptor.getElementName(idx)} cannot be parsed",
                                    err
                                )
                            }
                        }
                    Element.Structure(descriptor.serialName, children)
                }
                descriptor.isPolymorphic -> fromType("", descriptor)
                else -> error("${descriptor.serialName} is not supported")
            }
    }

    private fun fromType(parentName: String, descriptor: SerialDescriptor): Element {
        val name = descriptor.serialName
        val fullName = "$parentName.$name"

        return when {
            descriptor.isTrulyPrimitive -> Element.Primitive(name, descriptor.kind)
            descriptor.isString -> {
                val requiredLength = dfQueue.removeFirstOrNull()
                    ?: throw SerdeError.InsufficientLengthData(parentName, descriptor)
                Element.Strng(name, requiredLength)
            }
            descriptor.isCollection -> {
                val requiredLength = dfQueue.removeFirstOrNull()
                    ?: throw SerdeError.InsufficientLengthData(parentName, descriptor)
                val children = descriptor.elementDescriptors.map { fromType(fullName, it) }
                Element.Collection(name, inner = children, CollectionSizingInfo(requiredLength = requiredLength))
            }
            descriptor.isStructure ->  {
                val isAnnotated = (0 until descriptor.elementsCount)
                    .any { idx -> descriptor.getElementAnnotations(idx).isNotEmpty() }

                if (isAnnotated) {
                    parse(descriptor)
                } else {
                    val children = descriptor.elementDescriptors.map { fromType(fullName, it) }
                    Element.Structure(name, inner = children)
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

                val children = listOf(type, value).map { fromType(fullName, it) }

                Element.Structure(name, inner = children)

            }
            else -> error("Unreachable code when building element from type ${descriptor.serialName}")
        }
    }
}