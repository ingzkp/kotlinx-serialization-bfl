package com.ing.serialization.bfl.serde

import com.ing.serialization.bfl.prepend
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.modules.SerializersModule

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
}
