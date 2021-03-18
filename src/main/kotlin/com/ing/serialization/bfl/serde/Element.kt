package com.ing.serialization.bfl.serde

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.encoding.AbstractEncoder
import java.io.DataOutput

/**
 * The basic abstraction of each object being serialized.
 */
@ExperimentalSerializationApi
sealed class Element(val name: String) {
    abstract val layout: Layout
    open val size by lazy {
        layout.mask.sumBy { it.second }
    }
    abstract val isNullable: Boolean
    val nullLayout by lazy {
        if (isNullable) { listOf(Pair("nonNull", 1)) } else { listOf() }
    }

        /**
         * Primitive case.
         */
         class Primitive(name: String, private val kind: SerialKind, override val isNullable: Boolean): Element(name) {
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

            Layout(name, nullLayout + listOf(Pair("value", size)), listOf())
        }

        fun encodeNull(encoder: AbstractEncoder) =
            with(encoder) {
                when (kind) {
                    is PrimitiveKind.BOOLEAN -> encodeBoolean(false)
                    is PrimitiveKind.BYTE -> encodeByte(0)
                    is PrimitiveKind.SHORT -> encodeShort(0)
                    is PrimitiveKind.INT -> encodeInt(0)
                    is PrimitiveKind.LONG -> encodeLong(0)
                    is PrimitiveKind.FLOAT -> throw IllegalStateException("Floats are not yet supported")
                    is PrimitiveKind.DOUBLE -> throw IllegalStateException("Double are not yet supported")
                    is PrimitiveKind.CHAR -> encodeChar('\u0000')
                    else -> throw IllegalStateException("$name is called primitive while it is not")
                }
            }
    }

    /**
     * String case.
     */
    class Strng(name: String, val requiredLength: Int, override val isNullable: Boolean) : Element(name) {
        override val layout by lazy {
            // BOOLEAN (if nullable; value present -> 1) + SHORT (string length) + requiredLength * length(CHAR)

            val layout = listOf(
                Pair("length", 2),
                Pair("value", requiredLength * 2)
            )

            Layout(name, nullLayout + layout, listOf())
        }

        /**
         * Returns the number of bytes the string to be padded.
         *
         * @throws SerdeError.StringTooLarge exception when string doesn't fit its given limit
         */
        private fun padding(actualLength: Int): Int {
            if (requiredLength < actualLength)
                throw SerdeError.StringTooLarge(actualLength, this)

            return 2 * (requiredLength - actualLength)
        }

        fun encode(string: String?, encoder: AbstractEncoder) {
            val actualLength = string?.length ?: 0

                // In output.writeUTF, length of the string is stored as short.
                // We do the same for consistency.
                encoder.encodeShort(actualLength.toShort())
                string?.forEach { encoder.encodeChar(it) }
                repeat(padding(actualLength)) { encoder.encodeByte(0) }
            }

        fun encodeNull(encoder: AbstractEncoder) = encode(null, encoder)

        fun decode(decoder: BinaryFixedLengthInputDecoder): String {
            // In output.writeUTF, length of the string is stored as short.
            // We do the same for consistency.
            val actualLength = decoder.decodeShort().toInt()
            val string = (0 until actualLength).map { decoder.decodeChar() }.joinToString("")
            decoder.skipBytes(padding(actualLength))

            return string
        }
    }

    /**
     * Lists and Maps.
     */
    class Collection(
        name: String,
        val inner: List<Element>,
        private val sizingInfo: CollectionSizingInfo,
        override val isNullable: Boolean
    ) : CollectionElementSizingInfo by sizingInfo, Element(name) {
        override val layout by lazy {
            // INT (collection length) + number_of_elements * sum_i { size(inner_i) }
            // = 4 + n * sum_i { size(inner_i) }
            val layout = listOf(
                Pair("length", 4),
                Pair("value", requiredLength * elementSize)
            )

            Layout(name, nullLayout + layout, inner.map { it.layout })
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
    }

    /**
     * All other cases.
     */
    class Structure(name: String, val inner: List<Element>, override val isNullable: Boolean) : Element(name) {
        override val layout by lazy {
            val layout = listOf(Pair("length", size))
            Layout(name, nullLayout + layout, inner.map { it.layout })
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
