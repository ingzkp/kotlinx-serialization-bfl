package serde

import annotations.DFLength
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementDescriptors

@ExperimentalSerializationApi
sealed class Element(val name: String) {
    class Primitive(name: String): Element(name)

    class Strng(name: String, val requiredLength: Int): Element(name)

    // To be used to describe Collections (List/Map)
    class Collection(name: String, val sizingInfo: CollectionSizingInfo): CollectionElementSizingInfo by sizingInfo, Element(name)

    class Structure(name: String, val inner: List<Element>, var isResolved: Boolean): Element(name)

    // TODO why copy?
    fun copy(): Element  = when (this) {
        is Primitive -> this
        is Strng -> this
        is Collection -> Collection(name, sizingInfo.copy())
        is Structure -> Structure(name, ArrayList(inner), isResolved)
    }

    inline fun <reified T: Element> expect(): T {
        // Non-null assertion is fine because T is bound to Element.
        (this as? T) ?: throw SerdeError.WrongElement(T::class.simpleName!!, this)
        return this
    }

    @ExperimentalSerializationApi
    companion object {
        const val polySerialNameLength = 100

        fun fromProperty(containerDescriptor: SerialDescriptor, propertyIdx: Int): Element {
            val descriptor = containerDescriptor.getElementDescriptor(propertyIdx)
            val name = "${containerDescriptor.serialName}.${descriptor.serialName}"

            return when {
                descriptor.isTrulyPrimitive -> Primitive(name)
                descriptor.isCollection || descriptor.isString -> {
                    // Top-level Collection or String must have annotations
                    val lengths = containerDescriptor.getElementAnnotations(propertyIdx)
                        .filterIsInstance<DFLength>()
                        .firstOrNull()?.lengths?.toList()?.let { ArrayDeque(it) }
                        ?: throw SerdeError.AbsentAnnotations(containerDescriptor, propertyIdx)

                    fromType(containerDescriptor.serialName, descriptor, lengths)
                }
                descriptor.isStructure -> {
                    // Classes may have inner collections buried deep inside.
                    // We can infer this by observing an appropriate length annotation.
                    val lengths = containerDescriptor.getElementAnnotations(propertyIdx)
                        .filterIsInstance<DFLength>()
                        .firstOrNull()?.lengths?.toList()?.let { ArrayDeque(it) }
                    if (lengths != null) {
                        fromType(containerDescriptor.serialName, descriptor, lengths)
                    } else {
                        Structure(name, inner = listOf(), isResolved = false)
                    }
                }
                descriptor.isPolymorphic || descriptor.isContextual -> {
                    fromType(containerDescriptor.serialName, descriptor)
                }
                else -> error("Error processing property ${descriptor.serialName}")
            }
        }

        fun fromType(parentName: String, descriptor: SerialDescriptor, lengths: ArrayDeque<Int> = ArrayDeque()): Element {
            val name = "$parentName.${descriptor.serialName}"

            return when {
                descriptor.isTrulyPrimitive -> Primitive(name)
                descriptor.isString -> {
                    val requiredLength = lengths.removeFirstOrNull()
                        ?: throw SerdeError.InsufficientLengthData(parentName, descriptor)
                    Strng(name, requiredLength)
                }
                descriptor.isCollection -> {
                    val requiredLength = lengths.removeFirstOrNull()
                        ?: throw SerdeError.InsufficientLengthData(parentName, descriptor)
                    val children = descriptor.elementDescriptors.map {
                        fromType(name, it, lengths)
                    }
                    Collection(name, CollectionSizingInfo(requiredLength = requiredLength, inner = children))
                }
                descriptor.isStructure ->  {
                    val children = descriptor.elementDescriptors.map {
                        fromType(name, it, lengths)
                    }
                    Structure(name, inner = children, isResolved = true)
                }
                descriptor.isPolymorphic -> {
                    // Polymorphic type consists of a string and a structure.
                    lengths.addFirst(polySerialNameLength)
                    val children = descriptor.elementDescriptors.map { fromType(name, it, lengths) }

                    Structure(name, inner = children, isResolved = true)
                }
                descriptor.isContextual -> Structure(name, inner = listOf(), isResolved = false)
                else -> error("Unreachable code when building element from type ${descriptor.serialName}")
            }
        }
    }
}

@ExperimentalSerializationApi
interface CollectionElementSizingInfo {
    var startByte: Int?
    var actualLength: Int?
    var requiredLength: Int
    var inner: List<Element>
}

@ExperimentalSerializationApi
data class CollectionSizingInfo(
    override var startByte: Int? = null,
    override var actualLength: Int? = null,
    override var requiredLength: Int,
    override var inner: List<Element> = mutableListOf(),
) : CollectionElementSizingInfo
