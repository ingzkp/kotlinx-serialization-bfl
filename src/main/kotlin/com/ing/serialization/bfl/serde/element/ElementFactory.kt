package com.ing.serialization.bfl.serde.element

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.serialization.bfl.serde.SerdeError
import com.ing.serialization.bfl.serde.convertToList
import com.ing.serialization.bfl.serde.getPropertyNameValuePair
import com.ing.serialization.bfl.serde.isCollection
import com.ing.serialization.bfl.serde.isContextual
import com.ing.serialization.bfl.serde.isEnum
import com.ing.serialization.bfl.serde.isPolymorphic
import com.ing.serialization.bfl.serde.isString
import com.ing.serialization.bfl.serde.isStructure
import com.ing.serialization.bfl.serde.isTrulyPrimitive
import com.ing.serialization.bfl.serde.merge
import com.ing.serialization.bfl.serde.prepend
import com.ing.serialization.bfl.serde.simpleSerialName
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.capturedKClass
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.descriptors.getContextualDescriptor
import kotlinx.serialization.descriptors.getPolymorphicDescriptors
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlin.collections.ArrayDeque
import kotlin.reflect.KClass

class ElementFactory(
    private val serializersModule: SerializersModule = EmptySerializersModule,
    outerFixedLength: IntArray = IntArray(0)
) {
    private var dfQueue = ArrayDeque(outerFixedLength.toList())

    // stack-like structure used to cache the content of dfQueue before parsing each child of a CollectionElement - in
    // that way each child has the same view on the dfQueue before its parsing starts
    private var dfQueueSnapshots = ArrayDeque<ArrayDeque<Int>>()

    /**
     * Parses a class structure property by property recursively.
     * If withPropertyName is not null, this class structure is a property of some other wrapping structure.
     * If data is not null, an instance of the class has been provided too - This option is used before encoding for
     * the implementations of polymorphic types to be resolved properly
     */
    fun parse(descriptor: SerialDescriptor, withPropertyName: String? = null, data: Any? = null): Element {
        val parentName = withPropertyName ?: descriptor.simpleSerialName

        return when {
            descriptor.isTrulyPrimitive ||
                descriptor.isString ||
                descriptor.isEnum ||
                descriptor.isPolymorphic ||
                descriptor.isCollection -> fromType(descriptor, parentName, data)

            descriptor.isStructure -> {
                val children = (0 until descriptor.elementsCount)
                    .map { idx ->
                        val (propertyName, propertyValue) = data.getPropertyNameValuePair(descriptor, idx)

                        val lengths = descriptor.getElementAnnotations(idx)
                            .filterIsInstance<FixedLength>()
                            .firstOrNull()?.lengths?.toList()?.let { ArrayDeque(it) }
                            ?: listOf()
                        dfQueue.prepend(lengths)

                        fromType(descriptor.getElementDescriptor(idx), "$parentName.$propertyName", propertyValue)
                    }.toMutableList()
                StructureElement(descriptor.serialName, parentName, children, descriptor.isNullable).apply {
                    isNull = data == null // set flag if the instance is null
                }
            }

            else -> error("${descriptor.serialName} is not supported")
        }
    }

    @Suppress("ComplexMethod", "LongMethod", "NestedBlockDepth")
    private fun fromType(descriptor: SerialDescriptor, parentName: String, data: Any? = null): Element {
        val serialName = descriptor.serialName

        return when {
            descriptor.isTrulyPrimitive -> PrimitiveElement(serialName, parentName, descriptor.kind, descriptor.isNullable)
            descriptor.isString -> {
                val requiredLength = dfQueue.removeFirstOrNull()
                    ?: throw SerdeError.InsufficientLengthData(descriptor, parentName)
                StringElement(serialName, parentName, requiredLength, descriptor.isNullable)
            }
            descriptor.isEnum -> EnumElement(serialName, parentName, descriptor.isNullable)
            descriptor.isCollection -> {
                val requiredLength = dfQueue.removeFirstOrNull()
                    ?: throw SerdeError.InsufficientLengthData(descriptor, parentName)

                // first try to treat data as list using reflection
                val children = data?.convertToList()?.let {
                    // in case of an empty collection, don't pass any data to a deeper parsing level
                    if (it.isEmpty()) {
                        return@let descriptor.elementDescriptors.map { innerDescriptor -> fromType(innerDescriptor, parentName) }.toMutableList()
                    }
                    // if the collection is not empty, it can be of either of List-like or Map-like type
                    if (data is Map<*, *> || data is MutableMap<*, *>) {
                        it.filterIsInstance<Pair<*, *>>().unzip().toList()
                    } else {
                        listOf(it)
                    }.zip(descriptor.elementDescriptors)
                        .map { (inner, innerDescriptor) -> inner.resolveChildrenTypes(innerDescriptor, parentName) }
                        .toMutableList()
                } ?: descriptor.elementDescriptors.map { fromType(it, parentName) }.toMutableList()

                CollectionElement(
                    serialName = serialName,
                    propertyName = parentName,
                    inner = children,
                    requiredLength = requiredLength,
                    isNullable = descriptor.isNullable
                ).apply {
                    isNull = data == null // set flag if the instance is null
                }
            }
            descriptor.isStructure -> {
                val isAnnotated = (0 until descriptor.elementsCount)
                    .any { idx -> descriptor.getElementAnnotations(idx).isNotEmpty() }

                if (isAnnotated) {
                    parse(descriptor, parentName, data)
                } else {
                    val children = descriptor.elementDescriptors.mapIndexed { idx, element ->
                        val (propertyName, propertyValue) = data.getPropertyNameValuePair(descriptor, idx)
                        fromType(element, "$parentName.$propertyName", propertyValue)
                    }.toMutableList()

                    StructureElement(serialName, parentName, children, descriptor.isNullable).apply {
                        isNull = data == null // set flag if the instance is null
                    }
                }
            }
            descriptor.isPolymorphic -> {
                // Check if there is a descriptor for the polymorphic type.
                val polyDescriptors = serializersModule.getPolymorphicDescriptors(descriptor)
                if (polyDescriptors.isEmpty()) {
                    throw SerdeError.NoPolymorphicSerializers(descriptor)
                }

                // serialName's for all variant of the polymorphic type must have the same length
                // to produce a fixed length serialization.
                val variantNamesLengths = polyDescriptors.map { it.serialName.length }.distinct()
                if (variantNamesLengths.size != 1) {
                    throw SerdeError.VariablePolymorphicSerialName(descriptor)
                }
                dfQueue.prepend(variantNamesLengths.single())
                // retrieve the base class of the polymorphic and cast it appropriately
                val polyBaseClass: KClass<in Any> = descriptor.capturedKClass as? KClass<in Any>
                    ?: throw SerdeError.NoPolymorphicBaseClass(descriptor.serialName)
                // Polymorphic type consists of a string describing type and a structure.
                val children = mutableListOf(
                    // Inner StringElement
                    fromType(descriptor.elementDescriptors.first(), parentName),
                    // Inner StructureElement - The serializer of the class implementing the base polymorphic MUST (!!!)
                    // inherit from SurrogateSerializer
                    data?.let {
                        // retrieve the serializer and cast it as SurrogateSerializer so that the actual data value can
                        // be transformed to its surrogate version using `toSurrogate`
                        val valueSerializer = (
                            serializersModule.getPolymorphic(polyBaseClass, it)
                                ?: throw SerdeError.NoPolymorphicSerializerForSubClass(it::class.toString())
                            ) as? SurrogateSerializer<Any, Surrogate<Any>>
                            ?: throw SerdeError.NoSurrogateSerializerForPolymorphic(it::class.toString())
                        fromType(valueSerializer.descriptor, parentName, valueSerializer.toSurrogate(it))
                    } ?: StructureElement("", parentName, mutableListOf(), descriptor.isNullable)
                )

                StructureElement(serialName, parentName, children, descriptor.isNullable).apply {
                    isPolymorphic = true // denote the element as polymorphic
                    isNull = data == null // set flag if the instance is null
                    baseClass = polyBaseClass // pass the base class so that it can be used during decoding of null polymorphic
                }
            }
            descriptor.isContextual -> {
                val contextDescriptor = serializersModule.getContextualDescriptor(descriptor)
                    ?: throw SerdeError.NoContextualSerializer(descriptor)

                fromType(contextDescriptor, parentName, data).apply { isNullable = descriptor.isNullable }
            }
            else -> error("Do not know how to build element from type ${descriptor.serialName}")
        }
    }

    /**
     * Extension function for parsing all children of a Collection and finally merging all parsed elements into a single
     * one
     * @param descriptor serial descriptor of the elements to be parsed
     * @param parentName name of the parent element
     */
    private fun <T> List<T>.resolveChildrenTypes(descriptor: SerialDescriptor, parentName: String): Element = this
        .mapIndexed { idx, inner ->
            // before the first element of the list is parsed, cache the contents of the dfQueue to make them available
            // to the rest elements of the list
            if (idx == 0) dfQueueSnapshots.add(ArrayDeque(dfQueue))
            fromType(descriptor, parentName, inner).also {
                // upon parsing reset dfQueue to its initial value if the total parsing of the list has not been completed
                // or remove it from the cache if all the elements in the list have been parsed
                if (idx != this.size - 1) {
                    dfQueue = ArrayDeque(dfQueueSnapshots.last())
                } else {
                    dfQueueSnapshots.removeLast()
                }
            }
        }
        .reduce { element1, element2 -> element1.merge(element2) }
}
