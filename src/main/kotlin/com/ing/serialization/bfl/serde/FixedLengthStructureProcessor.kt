package com.ing.serialization.bfl.serde

import com.ing.serialization.bfl.serde.element.CollectionElement
import com.ing.serialization.bfl.serde.element.Element
import com.ing.serialization.bfl.serde.element.ElementFactory
import com.ing.serialization.bfl.serde.element.StructureElement
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.modules.SerializersModule

class FixedLengthStructureProcessor(
    descriptor: SerialDescriptor,
    serializersModule: SerializersModule,
    outerFixedLength: IntArray = IntArray(0)
) {

    internal var structure: Element = ElementFactory(serializersModule, outerFixedLength).parse(descriptor)
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
        val schedulable = queue.first().expect<StructureElement>()

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

        repeat(collectionSize) {
            queue.prepend(collection.inner)
        }
    }
}
