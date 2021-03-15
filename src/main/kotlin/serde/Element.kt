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
    class Primitive(name: String, private val kind: SerialKind): Element(name) {
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
    class Strng(name: String, val requiredLength: Int): Element(name) {
        override val layout by lazy {
            // SHORT (string length) + requiredLength * length(CHAR)
            Layout(name,
                listOf(
                    Pair("length", 2),
                    Pair("value", requiredLength * 2)
                ),
                listOf()
            )
        }
    }

    /**
     * Lists and Maps.
     */
    class Collection(name: String, val inner: List<Element>, private val sizingInfo: CollectionSizingInfo):
        CollectionElementSizingInfo by sizingInfo, Element(name)
    {
        override val layout by lazy {
            // INT (collection length) + number_of_elements * sum_i { size(inner_i) }
            // = 4 + n * sum_i { size(inner_i) }
            Layout(name,
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
    }

    /**
     * All other cases.
     */
    class Structure(name: String, val inner: List<Element>) : Element(name) {
        override val layout by lazy {
            Layout(name,
                listOf(Pair("length", size)),
                inner.map { it.layout }
            )
        }

        override val size by lazy {
            inner.sumBy { it.size }
        }
    }

    inline fun <reified T: Element> expect(): T {
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
        return "$prefix$name\n$deepPrefix"+
            mask.joinToString(separator = "\n$deepPrefix") { "${it.first} - ${it.second}" } +
            "\n" +
            if (inner.isNotEmpty()) { inner.joinToString(separator = "") { it.toString(deepPrefix) } } else { "" }
    }
}