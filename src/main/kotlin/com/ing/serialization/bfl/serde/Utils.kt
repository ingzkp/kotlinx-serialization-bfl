package com.ing.serialization.bfl.serde

import com.ing.serialization.bfl.serde.element.Element
import com.ing.serialization.bfl.serde.element.ElementFactory
import com.ing.serialization.bfl.serde.element.StructureElement
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.modules.SerializersModule
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

fun <T> ArrayDeque<T>.prepend(value: T) {
    addFirst(value)
}

fun <T> ArrayDeque<T>.prepend(list: List<T>) {
    addAll(0, list)
}

/**
 * Extension function that attempts to parse any iterable data structure as a list using reflection
 * Throws an exception if the data structure is of none of the supported types
 *
 * @throws IllegalStateException when the actual type of the data structure is none of the supported
 */
@Suppress("ComplexMethod")
fun <T> T.convertToList() = when (this) {
    is Collection<*> -> this.toList()
    is MutableCollection<*> -> this.toList()
    is Map<*, *> -> this.toList()
    is MutableMap<*, *> -> this.toList()
    is Array<*> -> this.toList()
    is ByteArray -> this.toList()
    is CharArray -> this.toList()
    is ShortArray -> this.toList()
    is IntArray -> this.toList()
    is LongArray -> this.toList()
    is DoubleArray -> this.toList()
    is FloatArray -> this.toList()
    is BooleanArray -> this.toList()
    else -> error("Unknown iterable type - Cannot convert to list!")
}

/**
 * Extension function that retrieves the name and value of a specific property from a data structure
 * @param descriptor serial descriptor of the data object
 * @param index index of the property within the structure of the data object
 */
fun <T> T.getPropertyNameValuePair(descriptor: SerialDescriptor, index: Int): Pair<String, Any?> {
    val propertyName = descriptor.getElementName(index)
    val propertyValue = this?.let {
        it::class.memberProperties
            .firstOrNull { property -> property.name == propertyName }
            ?.also { property -> property.isAccessible = true } // in case some property is private or protected
            ?.call(it)
    }
    return Pair(propertyName, propertyValue)
}

/**
 * Extension function that merges an element with another element of the same type (externally respected condition)
 * Used for merging the children of a CollectionElement into one single Element type.
 *
 * @param other the element to be merged with
 * @throws IllegalArgumentException when the elements to be merged are not of the same type
 * @throws SerdeError.DifferentPolymorphicImplementations when different implementations of the same polymorphic base
 * type are encountered
 */
fun Element.merge(other: Element): Element {
    require(this::class == other::class) { "Elements to be merged should be of the same type" }
    return if (isNull || other.isNull) {
        if (isNull) other.clone() else this.clone()
    } else {
        // Polymorphic type consists of a string describing type and a structure
        if (isPolymorphic && inner.last().serialName != other.inner.last().serialName) {
            throw SerdeError.DifferentPolymorphicImplementations(serialName)
        }
        inner = inner.mapIndexed { idx, child -> child.merge(other.inner[idx]) }.toMutableList()
        this.clone()
    }
}

/**
 * Extension function used for resolving the inner StructureElement of a polymorphic when its actual type is available
 *
 * @param descriptor the serial descriptor of the implementation of the base class
 * @param propertyName the property name to be used when parsing the actual inner StructureElement
 * @param serializersModule the collection of known serializers
 */
fun Element.resolvePolymorphicChild(
    descriptor: SerialDescriptor,
    propertyName: String,
    serializersModule: SerializersModule
): StructureElement =
    ElementFactory(serializersModule)
        .parse(descriptor, propertyName)
        .expect<StructureElement>()
        .also {
            it.parent = this
            // update the parent with the newly created child (done for completeness of the elements' structure)
            val ind = this.inner.indexOfFirst { element -> element is StructureElement }
            this.inner[ind] = it
        }
