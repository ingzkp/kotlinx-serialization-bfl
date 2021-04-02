package com.ing.serialization.bfl.serde.element

import com.ing.serialization.bfl.serde.SerdeError
import java.io.DataOutput

/**
 * The basic abstraction of each object being serialized.
 */

class CollectionElement(
    serialName: String,
    propertyName: String,
    inner: List<Element>,
    private val sizingInfo: CollectionSizingInfo,
    override val isNullable: Boolean
) : CollectionElementSizingInfo by sizingInfo, Element(serialName, propertyName, inner) {
    /**
     * INT (collection length) + number_of_elements * sum_i { size(inner_i) }
     * = 4 + n * sum_i { size(inner_i) }
     */
    override val inherentLayout by lazy {
        listOf(
            Pair("length", 4),
            Pair("value", requiredLength * elementSize)
        )
    }

    private val elementSize by lazy {
        inner.sumBy { it.size }
    }

    /**
     * The number of bytes the collection to be padded. It is always different and depends on the state.
     *
     * @throws SerdeError.CollectionNoActualLength exception when length of a collection is not specified.
     * @throws SerdeError.CollectionTooLarge exception when collection doesn't fit into the given limit
     */
    val padding: Int
        get() {
            val actualLength = actualLength ?: throw SerdeError.CollectionNoActualLength(this)

            if (requiredLength < actualLength) {
                throw SerdeError.CollectionTooLarge(this)
            }
            return elementSize * (requiredLength - actualLength)
        }

    override fun encodeNull(output: DataOutput) =
        repeat(4 + requiredLength * elementSize) { output.writeByte(0) }
}

interface CollectionElementSizingInfo {
    var actualLength: Int?
    val requiredLength: Int
}

data class CollectionSizingInfo(
    override var actualLength: Int? = null,
    override val requiredLength: Int,
) : CollectionElementSizingInfo
