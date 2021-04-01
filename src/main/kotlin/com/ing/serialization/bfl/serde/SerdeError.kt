package com.ing.serialization.bfl.serde

import com.ing.serialization.bfl.serde.element.CollectionElement
import com.ing.serialization.bfl.serde.element.Element
import com.ing.serialization.bfl.serde.element.StringElement
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlin.reflect.KClass

sealed class SerdeError(message: String) : IllegalStateException(message) {

    class Unreachable(message: String) : SerdeError("Panic. Unreachable code. $message")

    class NonPrimitive(kind: SerialKind) : SerdeError("$kind is not a primitive type")

    class UnexpectedElement(expected: String, actual: Element) : SerdeError("Expected $expected, actual ${actual.name}")

    class StringTooLarge(actualLength: Int, element: StringElement) :
        SerdeError("Size of ${element.name} ($actualLength) is larger than required (${element.requiredLength})")

    class CollectionNoActualLength(element: CollectionElement) :
        SerdeError("StructureElement `${element.name}` does not specify its actual length")

    class CollectionTooLarge(element: CollectionElement) :
        SerdeError("Size of ${element.name} (${element.actualLength}) is larger than required (${element.requiredLength})")

    class InsufficientLengthData(descriptor: SerialDescriptor, parentName: String) :
        SerdeError("Insufficient length data along the chain $parentName.${descriptor.simpleSerialName}")

    class NoPolymorphicSerializers(descriptor: SerialDescriptor) :
        SerdeError("Serializers module has no serializers for a polymorphic type ${descriptor.serialName}")

    class NoContextualSerializer(descriptor: SerialDescriptor) :
        SerdeError("Serializers module has no serializers for a context type ${descriptor.serialName}")

    class NoTopLevelSerializer(klass: KClass<*>) :
        SerdeError("Top-level serializer absent for ${klass.simpleName}")

    class CannotDeserializeAs(data: ByteArray, klass: KClass<*>) :
        SerdeError("Cannot deserialize bytes as ${klass.simpleName}")
}
