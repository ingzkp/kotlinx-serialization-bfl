package com.ing.serialization.bfl.serde

import com.ing.serialization.bfl.serde.element.CollectionElement
import com.ing.serialization.bfl.serde.element.Element
import com.ing.serialization.bfl.serde.element.EnumElement
import com.ing.serialization.bfl.serde.element.PrimitiveElement
import com.ing.serialization.bfl.serde.element.StringElement
import com.ing.serialization.bfl.serde.element.StructureElement
import kotlinx.serialization.descriptors.SerialDescriptor
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
    // Class.forName(descriptor.serialName).getConstructor(this!!::class.java).newInstance(this)
    val propertyValue = this?.let {
        it::class.memberProperties
            // this search is based on the assumption that surrogates use serial names for their properties same to the
            // actual names of the properties
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
 * @throws IllegalStateException when an unknown element type is encountered (should be unreachable)
 */
fun Element.merge(other: Element): Element = when (this) {
    is PrimitiveElement -> {
        require(other is PrimitiveElement) { "Elements to be merged should be of the same type" }
        this.clone()
    }
    is EnumElement -> {
        require(other is EnumElement) { "Elements to be merged should be of the same type" }
        this.clone()
    }
    is StringElement -> {
        require(other is StringElement) { "Elements to be merged should be of the same type" }
        this.clone()
    }
    is StructureElement -> {
        require(other is StructureElement) { "Elements to be merged should be of the same type" }
        this.mergeWithChildren(other)
    }
    is CollectionElement -> {
        require(other is CollectionElement) { "Elements to be merged should be of the same type" }
        this.mergeWithChildren(other)
    }
    else -> error("Unknown implementation of Element")
}

/**
 * Extension function supporting some special handling for merging 2 StructureElements
 *
 * @param other the StructureElement to be merged with
 * @throws IllegalStateException when different implementations of the same base type are encountered
 */
fun StructureElement.mergeWithChildren(other: StructureElement): StructureElement =
    if (isNull || other.isNull) {
        if (isNull) other.clone() else this.clone()
    } else {
        // Polymorphic type consists of a string describing type and a structure
        if (isPolymorphic && inner.last().serialName != other.inner.last().serialName) {
            error("Different implementations of the same base type are not allowed")
        }

        inner = inner.mapIndexed { idx, child -> child.merge(other.inner[idx]) }.toMutableList()

        StructureElement(serialName, propertyName, inner, isNullable).also {
            it.isPolymorphic = isPolymorphic
            it.isNull = isNull
            it.assignParentToChildren()
        }
    }

/**
 * Extension function supporting some special handling for merging 2 CollectionElements
 *
 * @param other the CollectionElement to be merged with
 */
fun CollectionElement.mergeWithChildren(other: CollectionElement): CollectionElement =
    if (isNull || other.isNull) {
        if (isNull) other.clone() else this.clone()
    } else {
        inner = inner.mapIndexed { idx, child -> child.merge(other.inner[idx]) }.toMutableList()

        CollectionElement(serialName, propertyName, inner, actualLength, requiredLength, isNullable).also {
            it.isNull = isNull
            it.assignParentToChildren()
        }
    }
