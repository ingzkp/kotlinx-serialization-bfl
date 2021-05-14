package com.ing.serialization.bfl.serde

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.serialization.bfl.serde.element.Element
import com.ing.serialization.bfl.serde.element.ElementFactory
import com.ing.serialization.bfl.serde.element.PolymorphicStructureElement
import com.ing.serialization.bfl.serde.element.StructureElement
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.modules.SerializersModule
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

fun <T> ArrayDeque<T>.prepend(value: T) {
    addFirst(value)
}

fun <T> ArrayDeque<T>.prepend(list: List<T>) {
    addAll(0, list)
}

fun <T> ArrayDeque<T>.reschedule(value: T) {
    removeFirst()
    prepend(value)
}

/**
 * Extension function that attempts to parse any iterable data structure as a list using reflection.
 *
 * It is necessary to distinguish between all these cases, since despite the fact that all of these classes are iterable,
 * they acquire this characteristic in different ways (Collection, MutableCollection by inheriting from Iterable, Arrays
 * by defining an iterator, and Map, MutableMap by defining an iterator on their entries).
 *
 * In case of Map or MutableMap, the data structure is parsed as a list of key-value pairs, which is further flattened
 * to a listOf(keys, values). As a result, in the case of simple list-like structures, a listOf(values) is returned.
 * This is a convenience step for further processing of the parsed data.
 *
 * Throws an exception if the data structure is of none of the supported types
 *
 * @throws IllegalStateException when the actual type of the data structure is none of the supported
 */
@Suppress("ComplexMethod")
fun <T> T.flattenToList() = when (this) {
    is Collection<*> -> listOf(this.toList())
    is MutableCollection<*> -> listOf(this.toList())
    is Map<*, *> -> this.toList().unzip().toList()
    is MutableMap<*, *> -> this.toList().unzip().toList()
    is Array<*> -> listOf(this.toList())
    is ByteArray -> listOf(this.toList())
    is CharArray -> listOf(this.toList())
    is ShortArray -> listOf(this.toList())
    is IntArray -> listOf(this.toList())
    is LongArray -> listOf(this.toList())
    is DoubleArray -> listOf(this.toList())
    is FloatArray -> listOf(this.toList())
    is BooleanArray -> listOf(this.toList())
    else -> error("Unknown iterable type - Cannot convert to list!")
}

/**
 * Extension function that attempts to cast a serializer as SurrogateSerializer
 *
 * @throws SerdeError.NoSurrogateSerializer when the attempted cast fails
 */
fun <T : Any> SerializationStrategy<T>.expect(klass: KClass<*>) = this as? SurrogateSerializer<Any, Surrogate<Any>>
    ?: throw SerdeError.NoSurrogateSerializer(klass)

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
            .also { property -> requireNotNull(property) { "Property of non-null data should have been present" } }
            ?.also { property -> property.isAccessible = true } // in case some property is private or protected
            ?.call(it)
    }
    return Pair(propertyName, propertyValue)
}

/**
 * Extension function that merges an element with another element of the same type (externally respected condition)
 * Used for merging the children of a CollectionElement into one single Element type.
 *
 * @param that the element to be merged with
 * @throws IllegalArgumentException when the elements to be merged are not of the same type
 * @throws SerdeError.DifferentPolymorphicImplementations when different implementations of the same polymorphic base
 * type are encountered
 */
fun Element.merge(that: Element): Element {
    require(this::class == that::class) { "Elements to be merged should be of the same type" }
    return if (this.isNull || that.isNull) {
        if (this.isNull) that.clone() else this.clone()
    } else {
        // polymorphic type consists of a string describing type and a structure
        if (this is PolymorphicStructureElement && this.inner.last().serialName != that.inner.last().serialName) {
            throw SerdeError.DifferentPolymorphicImplementations(this.serialName)
        }
        this.inner = this.inner.mapIndexed { idx, child -> child.merge(that.inner[idx]) }.toMutableList()
        this.clone()
    }
}

/**
 * Extension function used for resolving the inner StructureElement of a polymorphic when its actual SerialDescriptor is
 * available
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
