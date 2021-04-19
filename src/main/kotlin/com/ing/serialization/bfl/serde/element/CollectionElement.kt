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
    var actualLength: Int? = null,
    val requiredLength: Int,
    override var isNullable: Boolean
) : Element(serialName, propertyName, inner) {
    /**
     * INT (collection length) + number_of_elements * sum_i { size(inner_i) }
     * = 4 + n * sum_i { size(inner_i) }
     */
    override val inherentLayout by lazy {
        listOf(
            Pair("[Collection] original length byte-count", 4),
            Pair("[Collection] value length", requiredLength * elementSize)
        )
    }

    private val elementSize by lazy {
        inner.sumBy { it.size }
    }

    /**
     * The number of bytes the collection to be padded. It is always different and depends on the state.
     *
     * @throws SerdeError.CollectionTooLarge exception when collection doesn't fit into the given limit
     */
    val padding: Int
        get() {
            // a null actual length should never occur - encodeNull should have been called instead
            val actualLength = requireNotNull(actualLength) {
                "CollectionElement `$propertyName` ($serialName) does not specify its actual length"
            }

            if (requiredLength < actualLength) {
                throw SerdeError.CollectionTooLarge(this)
            }
            return elementSize * (requiredLength - actualLength)
        }

    override fun encodeNull(output: DataOutput) =
        repeat(4 + requiredLength * elementSize) { output.writeByte(0) }
}
