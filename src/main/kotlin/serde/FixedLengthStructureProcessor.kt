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

    fun getNextProcessed() = elementQueue.first()

    /**
     * Processes structure descriptor and stores it in queue.
     * If element has inner elements, they will be stored in the queue as well in the order as listed in inner field
     * of the structure.
     *
     * @param descriptor structure descriptor
     * @throws SerdeError.WrongElement exception when first element in queue is not a structure
     */
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

    /**
     * Retrieve the first element of the queue and store information about its inner elements the same amount time
     * as the size of collection.
     * Throws an exception if the first element of the queue is not a collection.
     *
     * @param collectionSize size of collection
     * @throws SerdeError.WrongElement exception when first element in queue is not a collection
     */
    fun beginCollection(collectionSize: Int) {
        val collection = elementQueue.first().expect<Element.Collection>()
        collection.actualLength = collectionSize

        repeat(collectionSize) {
            elementQueue.prepend(collection.inner.filter { it !is Element.Primitive })
        }
    }

    /**
     * The number of bytes the collection to be padded. It is always different and depends on the state.
     *
     * @throws SerdeError.WrongElement exception when first element in queue is not a collection
     */
    val collectionPadding: Int
        get() {
            val collection = elementQueue.removeFirst().expect<Element.Collection>()

            val collectionActualLength =
                collection.actualLength ?: throw SerdeError.CollectionNoActualLength(collection)
            val collectionRequiredLength = collection.requiredLength

            if (collectionRequiredLength < collectionActualLength) {
                throw SerdeError.CollectionTooLarge(collection)
            }

            return collection.elementSize * (collectionRequiredLength - collectionActualLength)
        }

    /**
     * Returns the number of bytes the string to be padded.
     *
     * @throws SerdeError.WrongElement exception when first element in queue is not a string
     * @throws SerdeError.StringTooLarge exception when string doesn't fit its given limit
     */
    fun stringPadding(actualLength: Int): Int {
        val string = elementQueue.removeFirst().expect<Element.Strng>()

        val requiredLength = string.requiredLength

        if (requiredLength < actualLength)
            throw SerdeError.StringTooLarge(actualLength, string)

        return 2 * (requiredLength - actualLength)
    }
}