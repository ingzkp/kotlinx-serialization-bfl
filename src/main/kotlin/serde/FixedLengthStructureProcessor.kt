package serde

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.modules.SerializersModule
import prepend

@ExperimentalSerializationApi
class FixedLengthStructureProcessor(private val serializersModule: SerializersModule) {
    private lateinit var structure: Element

    private var topLevel = true
    private val elementQueue = ArrayDeque<Element>()

    fun removeNextProcessed() = elementQueue.removeFirst()

    fun beginStructure(descriptor: SerialDescriptor) {
        val schedulable = if (topLevel) {
            topLevel = false
            structure = ElementFactory(serializersModule).parse(descriptor)
            // Place the element to the front of the queue.
            elementQueue.prepend(structure)
            structure
        } else {
            // TODO: add check if the struct on the stack coincides with the current descriptor.
            elementQueue.first()
        }.expect<Element.Structure>()

        // Unwind structure's inner elements to the queue.
        elementQueue.prepend(schedulable.inner.filter { it !is Element.Primitive })
    }

    fun beginCollection(collectionSize: Int) {
        val collection = elementQueue.first().expect<Element.Collection>()
        collection.actualLength = collectionSize

        repeat(collectionSize) {
            elementQueue.prepend(collection.inner.filter { it !is Element.Primitive })
        }
    }

    fun isLastElement(index: Int): Boolean = index == elementQueue.first().expect<Element.Collection>().actualLength

    val collectionPadding: Int
        get() {
        val collection = elementQueue.removeFirst().expect<Element.Collection>()

        val collectionActualLength = collection.actualLength ?: throw SerdeError.CollectionNoActualLength(collection)
        val collectionRequiredLength = collection.requiredLength

        if (collectionRequiredLength < collectionActualLength) {
            throw SerdeError.CollectionTooLarge(collection)
        }

        return collection.elementSize * (collectionRequiredLength - collectionActualLength)
    }

    fun stringPadding(actualLength: Int): Int {
        val string = elementQueue.removeFirst().expect<Element.Strng>()

        val requiredLength = string.requiredLength

        if (requiredLength < actualLength)
            throw SerdeError.StringTooLarge(actualLength, string)

        return 2 * (requiredLength - actualLength)
    }

    //See comment on the only usage of this method
    fun removeNextNonCollections() {
        while (elementQueue.first() !is Element.Collection) {
            elementQueue.removeFirst()
        }
    }
}