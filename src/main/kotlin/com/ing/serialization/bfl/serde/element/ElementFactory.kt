package com.ing.serialization.bfl.serde.element

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serde.SerdeError
import com.ing.serialization.bfl.serde.expect
import com.ing.serialization.bfl.serde.flattenToList
import com.ing.serialization.bfl.serde.getPropertyNameValuePair
import com.ing.serialization.bfl.serde.isCollection
import com.ing.serialization.bfl.serde.isContextual
import com.ing.serialization.bfl.serde.isEnum
import com.ing.serialization.bfl.serde.isObject
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
                descriptor.isObject ||
                descriptor.isCollection -> fromType(descriptor, parentName, data)

            descriptor.isStructure -> {
                val children = (0 until descriptor.elementsCount)
                    .map { idx ->
                        val (propertyName, propertyValue) = data?.toSurrogate().getPropertyNameValuePair(descriptor, idx)

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

    @Suppress("ComplexMethod")
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
            descriptor.isObject -> StructureElement(serialName, parentName, mutableListOf(), descriptor.isNullable)
            descriptor.isCollection -> fromCollection(descriptor, serialName, parentName, data)
            descriptor.isStructure -> fromStructure(descriptor, serialName, parentName, data)
            descriptor.isPolymorphic -> fromPolymorphic(descriptor, serialName, parentName, data)
            descriptor.isContextual -> {
                val contextDescriptor = serializersModule.getContextualDescriptor(descriptor)
                    ?: throw SerdeError.NoContextualSerializer(descriptor)

                fromType(contextDescriptor, parentName, data).apply { isNullable = descriptor.isNullable }
            }
            else -> error("Do not know how to build element from type ${descriptor.serialName}")
        }
    }

    private fun fromStructure(
        descriptor: SerialDescriptor,
        serialName: String,
        parentName: String,
        data: Any? = null
    ): Element {
        val isAnnotated = (0 until descriptor.elementsCount)
            .any { idx -> descriptor.getElementAnnotations(idx).isNotEmpty() }

        return if (isAnnotated) {
            parse(descriptor, parentName, data)
        } else {
            val children = descriptor.elementDescriptors.mapIndexed { idx, element ->
                val (propertyName, propertyValue) = data?.toSurrogate().getPropertyNameValuePair(descriptor, idx)
                fromType(element, "$parentName.$propertyName", propertyValue)
            }.toMutableList()

            StructureElement(serialName, parentName, children, descriptor.isNullable).apply {
                isNull = data == null // set flag if the instance is null
            }
        }
    }

    private fun fromCollection(
        descriptor: SerialDescriptor,
        serialName: String,
        parentName: String,
        data: Any? = null
    ): CollectionElement {
        val requiredLength = dfQueue.removeFirstOrNull()
            ?: throw SerdeError.InsufficientLengthData(descriptor, parentName)

        val children = (
            data?.flattenToList()?.zip(descriptor.elementDescriptors)?.map { (inner, innerDescriptor) ->
                inner.resolveChildrenTypes(innerDescriptor, parentName)
            } ?: descriptor.elementDescriptors.map { fromType(it, parentName) }
            ).toMutableList()

        return CollectionElement(
            serialName = serialName,
            propertyName = parentName,
            inner = children,
            requiredLength = requiredLength,
            isNullable = descriptor.isNullable
        ).apply {
            isNull = data == null // set flag if the instance is null
        }
    }

    private fun fromPolymorphic(
        descriptor: SerialDescriptor,
        serialName: String,
        parentName: String,
        data: Any? = null
    ): StructureElement {
        // check if there is a descriptor for the polymorphic type.
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

        // polymorphic type consists of a string describing type and a structure.
        val children = mutableListOf(
            // inner StringElement
            fromType(descriptor.elementDescriptors.first(), parentName),
            // inner StructureElement - the serializer of the class implementing the base polymorphic MUST (!!!)
            // inherit from SurrogateSerializer
            data?.let {
                // retrieve the serializer and cast it as SurrogateSerializer so that the actual data value can
                // be transformed to its surrogate version using `toSurrogate`
                val valueSerializer = (
                    serializersModule.getPolymorphic(polyBaseClass, it)
                        ?: throw SerdeError.NoPolymorphicSerializerForSubClass(it::class.toString())
                    ).expect(it::class)

                fromType(valueSerializer.descriptor, parentName, valueSerializer.toSurrogate(it))
            } ?: StructureElement("", parentName, mutableListOf(), descriptor.isNullable)
        )

        return PolymorphicStructureElement(serialName, parentName, children, descriptor.isNullable).apply {
            isNull = data == null // set flag if the instance is null
            baseClass = polyBaseClass // pass the base class so that it can be used during decoding of null polymorphic
        }
    }

    /**
     * Extension function that attempts to convert a non-null value to its respective surrogate using its contextual
     * serializer to do so.
     *
     * If a contextual serializer has not been registered, the value remains unchanged
     *
     */
    private fun <T : Any> T.toSurrogate() = serializersModule
        .getContextual(this::class)
        ?.expect(this::class)
        ?.toSurrogate
        ?.invoke(this) ?: this

    /**
     * Extension function for parsing all children of a Collection and finally merging all parsed elements into a single
     * one
     * @param descriptor serial descriptor of the elements to be parsed
     * @param parentName name of the parent element
     */
    private fun <T> List<T>.resolveChildrenTypes(descriptor: SerialDescriptor, parentName: String): Element =
        if (this.isEmpty()) {
            // in case of an empty collection, don't pass any data to a deeper parsing level
            fromType(descriptor, parentName)
        } else {
            this.mapIndexed { idx, inner ->
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
            }.reduce { element1, element2 -> element1.merge(element2) }
        }
}
