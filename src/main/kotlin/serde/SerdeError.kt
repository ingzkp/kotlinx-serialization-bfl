package serde

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor

@ExperimentalSerializationApi
sealed class SerdeError(message: String): IllegalStateException(message) {
    class WrongElement(expected: String, actual: Element): SerdeError("Expected $expected, actual ${actual.name}")

    class StringTooLarge(actualLength: Int, element: Element.Strng):
        SerdeError("Size of ${element.name} ($actualLength) is larger than required (${element.requiredLength})")

    class CollectionNoStart(element: Element.Collection):
        SerdeError("Structure `${element.name}` has no start byte index")
    class CollectionNoActualLength(element: Element.Collection):
        SerdeError("Structure `${element.name}` does not specify its actual length")
    class CollectionTooLarge(element: Element.Collection):
        SerdeError("Size of ${element.name} (${element.actualLength}) is larger than required (${element.requiredLength})")

    class CollectionSizingMismatch(element: Element.Collection, elementsCount: Int):
        SerdeError("Amount of sizing info (${element.inner.size}) does not match elements count ($elementsCount)")
    class StringSizingMismatch(stringActualLength: Int, stringRequiredLength: Int):
        SerdeError("String actual length ($stringActualLength) does not match the required length ($stringRequiredLength)")

    class AbsentAnnotations(container: SerialDescriptor, propertyIdx: Int):
        SerdeError("Property ${container.serialName}.${container.getElementName(propertyIdx)} has no length annotation")

    class InsufficientLengthData(parentName: String, descriptor: SerialDescriptor):
        SerdeError("Insufficient length data for $parentName.${descriptor.serialName}")
}