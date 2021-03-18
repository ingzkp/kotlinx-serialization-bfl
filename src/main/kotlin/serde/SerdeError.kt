package serde

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor

@ExperimentalSerializationApi
sealed class SerdeError : IllegalStateException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)

    class WrongElement(expected: String, actual: Element) : SerdeError("Expected $expected, actual ${actual.name}")

    class StringTooLarge(actualLength: Int, element: Element.Strng) :
        SerdeError("Size of ${element.name} ($actualLength) is larger than required (${element.requiredLength})")

    class CollectionNoActualLength(element: Element.Collection) :
        SerdeError("Structure `${element.name}` does not specify its actual length")

    class CollectionTooLarge(element: Element.Collection) :
        SerdeError("Size of ${element.name} (${element.actualLength}) is larger than required (${element.requiredLength})")

    class InsufficientLengthData(parentName: String, descriptor: SerialDescriptor) :
        SerdeError("Insufficient length data for $parentName.${descriptor.serialName}")

    class NoPolymorphicSerializers(descriptor: SerialDescriptor) :
        SerdeError("Serializers module has no serializers for a polymorphic type ${descriptor.serialName}")

    class CannotParse(message: String, cause: Throwable) : SerdeError(message, cause)
}
