package com.ing.serialization.bfl.serde

import com.ing.serialization.bfl.serde.element.CollectionElement
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor

@ExperimentalSerializationApi
sealed class SerdeError : IllegalStateException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)

    class WrongElement(expected: String, actual: Element) : SerdeError("Expected $expected, actual ${actual.name}")

    class StringTooLarge(actualLength: Int, element: StringElement) :
        SerdeError("Size of ${element.name} ($actualLength) is larger than required (${element.requiredLength})")

    class CollectionNoActualLength(element: CollectionElement) :
        SerdeError("StructureElement `${element.name}` does not specify its actual length")

    class CollectionTooLarge(element: CollectionElement) :
        SerdeError("Size of ${element.name} (${element.actualLength}) is larger than required (${element.requiredLength})")

    class InsufficientLengthData(parentName: String, descriptor: SerialDescriptor) :
        SerdeError("Insufficient length data for $parentName.${descriptor.serialName}")

    class NoPolymorphicSerializers(descriptor: SerialDescriptor) :
        SerdeError("Serializers module has no serializers for a polymorphic type ${descriptor.serialName}")

    class CannotParse(message: String, cause: Throwable) : SerdeError(message, cause)
}
