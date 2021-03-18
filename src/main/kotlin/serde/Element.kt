package serde

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialKind

/**
 * The basic abstraction of each object being serialized.
 */
@ExperimentalSerializationApi
sealed class Element(val name: String) {
    abstract val layout: Layout
    open val size by lazy {
        layout.mask.sumBy { it.second }
    }

    /**
     * Primitive case.
     */
    class Primitive(name: String, private val kind: SerialKind) : Element(name) {
        override val layout by lazy {
            val size = when (kind) {
                is PrimitiveKind.BOOLEAN -> 1
                is PrimitiveKind.BYTE -> 1
                is PrimitiveKind.SHORT -> 2
                is PrimitiveKind.INT -> 4
                is PrimitiveKind.LONG -> 8
                is PrimitiveKind.FLOAT -> throw IllegalStateException("Floats are not yet supported")
                is PrimitiveKind.DOUBLE -> throw IllegalStateException("Double are not yet supported")
                is PrimitiveKind.CHAR -> 2
                else -> throw IllegalStateException("$name is called primitive while it is not")
            }

            Layout(name, listOf(Pair("value", size)), listOf())
        }
    }

    /**
     * String case.
     */
    class Strng(name: String, val requiredLength: Int) : Element(name) {
        override val layout by lazy {
            // SHORT (string length) + requiredLength * length(CHAR)
            Layout(
                name,
                listOf(
                    Pair("length", 2),
                    Pair("value", requiredLength * 2)
                ),
                listOf()
            )
        }

        /**
         * Returns the number of bytes the string to be padded.
         *
         * @throws SerdeError.StringTooLarge exception when string doesn't fit its given limit
         */
        fun padding(actualLength: Int): Int {
            if (requiredLength < actualLength)
                throw SerdeError.StringTooLarge(actualLength, this)

            return 2 * (requiredLength - actualLength)
        }
    }

    /**
     * Lists and Maps.
     */
    class Collection(name: String, val inner: List<Element>, private val sizingInfo: CollectionSizingInfo) :
        CollectionElementSizingInfo by sizingInfo, Element(name) {
        override val layout by lazy {
            // INT (collection length) + number_of_elements * sum_i { size(inner_i) }
            // = 4 + n * sum_i { size(inner_i) }
            Layout(
                name,
                listOf(
                    Pair("length", 4),
                    Pair("value", requiredLength * elementSize)
                ),
                inner.map { it.layout }
            )
        }

        val elementSize by lazy {
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
    }

    /**
     * All other cases.
     */
    class Structure(name: String, val inner: List<Element>) : Element(name) {
        override val layout by lazy {
            Layout(
                name,
                listOf(Pair("length", size)),
                inner.map { it.layout }
            )
        }

        override val size by lazy {
            inner.sumBy { it.size }
        }
    }

    inline fun <reified T : Element> expect(): T {
        // Non-null assertion is fine because T is bound to Element.
        (this as? T) ?: throw SerdeError.WrongElement(T::class.simpleName!!, this)
        return this
    }
}

@ExperimentalSerializationApi
interface CollectionElementSizingInfo {
    var actualLength: Int?
    val requiredLength: Int
}

@ExperimentalSerializationApi
data class CollectionSizingInfo(
    override var actualLength: Int? = null,
    override val requiredLength: Int,
) : CollectionElementSizingInfo

class Layout(
    val name: String,
    val mask: List<Pair<String, Int>>,
    val inner: List<Layout>
) {
    fun toString(prefix: String = ""): String {
        val deepPrefix = "$prefix "
        return "$prefix$name\n$deepPrefix" +
            mask.joinToString(separator = "\n$deepPrefix") { "${it.first} - ${it.second}" } +
            "\n" +
            if (inner.isNotEmpty()) {
                inner.joinToString(separator = "") { it.toString(deepPrefix) }
            } else {
                ""
            }
    }
}
