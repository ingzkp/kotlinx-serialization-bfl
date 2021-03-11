package serde

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialKind

@ExperimentalSerializationApi
sealed class Element(val name: String) {
    abstract val size: Int

    class Primitive(name: String, private val kind: SerialKind): Element(name) {
        override val size by lazy {
            when (kind) {
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
        }
    }

    class Strng(name: String, val requiredLength: Int): Element(name) {
        override val size by lazy {
            // SHORT (string length) + requiredLength * length(CHAR)
            2 + requiredLength * 2
        }
    }

    // To be used to describe Collections (List/Map)
    class Collection(name: String, private val sizingInfo: CollectionSizingInfo):
        CollectionElementSizingInfo by sizingInfo, Element(name)
    {
        override val size by lazy {
            // INT (collection length) + number_of_elements * sum_i { size(inner_i) }
            // = 4 + n * sum_i { size(inner_i) }
            4 + requiredLength * elementSize
        }

        val elementSize by lazy {
            inner.sumBy { it.size }
        }
    }

    class Structure(name: String, val inner: List<Element>) : Element(name) {
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
    var requiredLength: Int
    var inner: List<Element>
}

@ExperimentalSerializationApi
data class CollectionSizingInfo(
    override var actualLength: Int? = null,
    override var requiredLength: Int,
    override var inner: List<Element> = mutableListOf(),
) : CollectionElementSizingInfo
