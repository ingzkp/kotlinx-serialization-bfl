package serde

import annotations.DFLength
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementDescriptors

sealed class Element(val name: String) {
    class Primitive(name: String): Element(name)

    // To be used to describe Collections (List/Map) and Strings
    class Collected(name: String, val sizingInfo: CollectedSizingInfo): ElementSizingInfo by sizingInfo, Element(name)

    class Structure(name: String, val inner: List<Element> = listOf(), var isResolved: Boolean): Element(name)

    fun copy(): Element  = when (this) {
        is Primitive -> this
        is Collected -> Collected(name, sizingInfo.copy())
        is Structure -> Structure(name, ArrayList(inner), isResolved)
    }

    @ExperimentalSerializationApi
    companion object {
        fun fromProperty(containerDescriptor: SerialDescriptor, propertyIdx: Int): Element {
            val descriptor = containerDescriptor.getElementDescriptor(propertyIdx)
            val name = "${containerDescriptor.serialName}.${descriptor.serialName}"

            return when {
                descriptor.isTrulyPrimitive -> Primitive(name)
                descriptor.isCollection || descriptor.isString -> {
                    // Top-level Collection or String must have annotations
                    val lengths = containerDescriptor.getElementAnnotations(propertyIdx)
                        .filterIsInstance<DFLength>()
                        .firstOrNull()?.lengths?.toMutableList()
                        ?: error("Element $name must have DFLength annotation")

                    fromType(containerDescriptor.serialName, descriptor, lengths)
                }
                descriptor.isStructure -> {
                    // Classes may have inner collections buried deep inside.
                    // We can infer this by observing an appropriate length annotation.
                    val lengths = containerDescriptor.getElementAnnotations(propertyIdx)
                        .filterIsInstance<DFLength>()
                        .firstOrNull()?.lengths?.toMutableList()
                    if (lengths != null) {
                        fromType(containerDescriptor.serialName, descriptor, lengths)
                    } else {
                        Structure(name, isResolved = false)
                    }
                }
                descriptor.isPolymorphic || descriptor.isContextual -> {
                    fromType(containerDescriptor.serialName, descriptor)
                }
                else -> error("Error processing property ${descriptor.serialName}")
            }
        }

        fun fromType(parentName: String, descriptor: SerialDescriptor, lengths: MutableList<Int> = mutableListOf()): Element {
            val name = "$parentName.${descriptor.serialName}"

            return when {
                descriptor.isTrulyPrimitive -> Primitive(name)
                descriptor.isCollection || descriptor.isString -> {
                    val requiredSize = lengths.removeFirstOrNull() ?: error("Insufficient sizing info for $name")
                    val children = descriptor.elementDescriptors.map {
                        fromType(name, it, lengths)
                    }
                    Collected(name,
                        CollectedSizingInfo(collectionRequiredLength = Length.Fixed(requiredSize), inner = children)
                    )
                }
                descriptor.isStructure ->  {
                    val children = descriptor.elementDescriptors.map {
                        fromType(name, it, lengths)
                    }
                    Structure(name, inner = children, isResolved = true)
                }
                descriptor.isPolymorphic -> {
                    // Polymorphic type consists of a string and a structure.
                    val children = listOf(
                        Collected(
                            "$name.${descriptor.getElementName(0)}",
                            CollectedSizingInfo(collectionRequiredLength = Length.Actual, inner = listOf())
                        )) +
                        descriptor.elementDescriptors.filterIndexed { idx, _ -> idx != 0 }.map {
                            fromType(name, it, lengths)
                        }

                    Structure(name, inner = children, isResolved = true)
                }
                descriptor.isContextual -> Structure(name, isResolved = false)
                else -> error("Unreachable code when building element from type ${descriptor.serialName}")
            }
        }
    }
}

interface ElementSizingInfo {
    var startByte: Int?
    var collectionActualLength: Int?
    var collectionRequiredLength: Length?
    var inner: List<Element>
}

data class CollectedSizingInfo(
    override var startByte: Int? = null,
    override var collectionActualLength: Int? = null,
    override var collectionRequiredLength: Length? = null,
    override var inner: List<Element> = mutableListOf(),
) : ElementSizingInfo

sealed class Length {
    object Actual: Length()
    data class Fixed(val value: Int): Length()
}