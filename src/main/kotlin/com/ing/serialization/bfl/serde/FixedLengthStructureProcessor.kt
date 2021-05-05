package com.ing.serialization.bfl.serde

import com.ing.serialization.bfl.serde.element.CollectionElement
import com.ing.serialization.bfl.serde.element.Element
import com.ing.serialization.bfl.serde.element.ElementFactory
import com.ing.serialization.bfl.serde.element.StructureElement
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.modules.SerializersModule

class FixedLengthStructureProcessor(
    descriptor: SerialDescriptor,
    val serializersModule: SerializersModule,
    outerFixedLength: IntArray = IntArray(0),
    data: Any? = null,
    private val phase: Phase = Phase.DECODING
) {
    internal var structure: Element = ElementFactory(serializersModule, outerFixedLength).parse(descriptor, data = data)
    private val queue = ArrayDeque<Element>()

    init {
        queue.prepend(structure)
    }

    fun removeNext() = queue.removeFirst()
    fun peekNext() = queue.first()

    /**
     * Processes structure descriptor and stores it in queue.
     * If element has inner elements, they will be stored in the queue as well in the order as listed in inner field
     * of the structure.
     *
     * @param descriptor structure descriptor
     * @throws SerdeError.UnexpectedElement exception when first element in queue is not a structure
     */
    fun beginStructure(descriptor: SerialDescriptor) {
        // TODO: add check if the struct on the stack coincides with the current descriptor.
        var schedulable = queue.first().expect<StructureElement>()

        // during decoding we need to populate the inner placeholder StructureElement of a polymorphic
        val parent = schedulable.parent
        if (parent != null && parent.isPolymorphic && phase == Phase.DECODING) {
            // populate the placeholder StructureElement
            schedulable = ElementFactory(serializersModule).parse(descriptor, schedulable.propertyName)
                .expect<StructureElement>()
                .also {
                    // update the parent with the newly created child
                    val ind = parent.inner.indexOfFirst { element -> element is StructureElement }
                    parent.inner[ind] = it
                }
            schedulable.parent = parent
            // remove the placeholder StructureElement from queue and add the populated version
            removeNext()
            queue.prepend(schedulable)
        }

        // Unwind structure's inner elements to the queue.
        queue.prepend(schedulable.inner)
    }

    /**
     * Retrieve the first element of the queue and store information about its inner elements the same amount time
     * as the size of collection.
     * Throws an exception if the first element of the queue is not a collection.
     *
     * @param collectionSize size of collection
     * @throws SerdeError.UnexpectedElement exception when first element in queue is not a collection
     */
    fun beginCollection(collectionSize: Int) {
        val collection = queue.first().expect<CollectionElement>()
        collection.actualLength = collectionSize

        // if an empty list containing nullable polymorphic has not been fully resolved in the parsing stage an exception is thrown
        if (collectionSize == 0 && phase == Phase.ENCODING) {
            collection.verifyResolvabilityOrThrow()
        }

        repeat(collectionSize) {
            queue.prepend(collection.inner)
        }
    }
}
