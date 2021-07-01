package com.ing.serialization.bfl.serde

import com.ing.serialization.bfl.serde.element.CollectionElement
import com.ing.serialization.bfl.serde.element.Element
import com.ing.serialization.bfl.serde.element.StringElement
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlin.reflect.KClass

sealed class SerdeError : IllegalStateException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)

    class NotFixedPrimitive(kind: SerialKind) : SerdeError("$kind is not a fixed length primitive type")

    class UnexpectedElement(expected: String, actual: Element) : SerdeError("Expected $expected, actual ${actual.serialName}")

    class UnexpectedPrimitive(expected: PrimitiveKind, actual: KClass<*>) : SerdeError("Expected $expected, actual ${actual.simpleName}")

    class StringTooLarge(actualLength: Int, element: StringElement) :
        SerdeError("Size of ${element.propertyName} (${element.serialName}) ($actualLength) is larger than required (${element.requiredLength})")

    class CollectionTooLarge(element: CollectionElement) :
        SerdeError("Size of ${element.propertyName} (${element.serialName}) (${element.actualLength}) is larger than required (${element.requiredLength})")

    class InsufficientLengthData(descriptor: SerialDescriptor, parentName: String) :
        SerdeError(
            "Could not determine fixed length information for every item in the chain of $parentName. " +
                "Please verify that all collections and strings in that chain are sufficiently annotated"
        )

    class DifferentPolymorphicImplementations(serialName: String) :
        SerdeError("Different implementations of the same base type '$serialName' are not allowed")

    class NoPolymorphicSerializerForSubClass(type: String) : SerdeError("Serializer absent for polymorphic subclass $type")

    class NoSurrogateSerializer(klass: KClass<*>) : SerdeError("Surrogate serializer absent for ${klass.qualifiedName}")

    class NoPolymorphicBaseClass(serialName: String) : SerdeError("Base class for '$serialName' cannot be found")

    class NonResolvablePolymorphic(serialName: String) : SerdeError("Implementation of '$serialName' cannot be inferred")

    class NoPolymorphicSerializers(descriptor: SerialDescriptor) :
        SerdeError("Serializers module has no serializers for a polymorphic type ${descriptor.serialName}")

    class VariablePolymorphicSerialName(descriptor: SerialDescriptor) :
        SerdeError("Variants of ${descriptor.serialName} have differently sized serial names")

    class NoContextualSerializer(descriptor: SerialDescriptor) :
        SerdeError("Serializers module has no serializers for a context type ${descriptor.serialName}")

    class NoTopLevelSerializer : SerdeError {
        constructor(clazz: Class<*>) : super("Top-level serializer absent for ${clazz.name}")
        constructor(klass: KClass<*>, cause: Throwable) : super("Top-level serializer absent for ${klass.qualifiedName}", cause)
    }
}
