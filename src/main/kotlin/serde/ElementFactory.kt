package serde

import annotations.DFLength
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
    var dfQueue = ArrayDeque<Int>()

    fun parse(descriptor: SerialDescriptor, parentName: String? = null): Element {
        return when {
            descriptor.isStructure -> {
                val inner = (0 until descriptor.elementsCount )
                    .map { idx ->
                        val lengths = descriptor.getElementAnnotations(idx)
                            .filterIsInstance<DFLength>()
                            .firstOrNull()?.lengths?.toList()?.let { ArrayDeque(it) }
                            ?: listOf()
                        dfQueue.prepend(lengths)

                        fromType(descriptor.serialName, descriptor.getElementDescriptor(idx))
                    }
                Element.Structure("${parentName ?: ""}${descriptor.serialName}", inner, isResolved = false)
            }
            descriptor.isPolymorphic -> fromType(descriptor.serialName, descriptor)
            else -> error("${descriptor.serialName} is not supported")
        }
    }

    fun fromType(parentName: String, descriptor: SerialDescriptor): Element {
        val name = "$parentName.${descriptor.serialName}"

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
                val children = descriptor.elementDescriptors.map { fromType(name, it) }
                Element.Collection(name, CollectionSizingInfo(requiredLength = requiredLength, inner = children))
            }
            descriptor.isStructure ->  {
                val isAnnotated = (0 until descriptor.elementsCount)
                    .any { idx -> descriptor.getElementAnnotations(idx).isNotEmpty() }

                if (isAnnotated) {
                    parse(descriptor, parentName)
                } else {
                    val children = descriptor.elementDescriptors.map { fromType(name, it) }
                    Element.Structure(name, inner = children, isResolved = true)
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
                    // todo add serde error
                    error("0 poly descriptors fpr ${descriptor.serialName}")
                }
                // TODO validate
                //  We work with fixed length serialization, thus
                //  we assume that all interfaces are serialized through the same surrogate or descriptor.
                //  thus any descriptor will do.
                val value = polyDescriptors.first()

                val children = listOf(type, value).map { fromType(name, it) }

                // val children = descriptor.elementDescriptors.map { fromType(name, it) }
                Element.Structure(name, inner = children, isResolved = true)

            }
            descriptor.isContextual -> {
                Element.Structure(name, inner = listOf(), isResolved = false)
            }
            else -> error("Unreachable code when building element from type ${descriptor.serialName}")
        }
    }

    private fun SerialDescriptor.unwind(): List<SerialDescriptor> {
        check(isPolymorphic) { "Only polymorphics can be unwound" }

        // Polymorphic type consists of a string and a structure.

        // Bound the serialName of the polymorphic type.
        val type = elementDescriptors.first()
        dfQueue.prepend(polySerialNameLength)

        // Get the descriptor for the polymorphic type.
        val polyDescriptors = serializersModule.getPolymorphicDescriptors(this)
        if (polyDescriptors.isEmpty()) {
            // todo add serde error
            error("0 poly descriptors fpr $serialName")
        }
        // TODO validate
        //  We work with fixed length serialization, thus
        //  we assume that all interfaces are serialized through the same surrogate or descriptor.
        //  thus any descriptor will do.
        val value = polyDescriptors.first()

       return listOf(type, value)
    }

    private fun List<Action>.flatten(): List<Element> =
        flatMap { when {
            it is Action.Merge && it.element is Element.Structure -> it.element.inner
            it is Action.Merge && it.element is Element.Collection -> it.element.inner
            it is Action.Keep -> listOf(it.element)
            else -> error("oh no")
        } }

    private sealed class Action(val element: Element) {
        class Merge(element: Element): Action(element)
        class Keep(element: Element): Action(element)
    }
}