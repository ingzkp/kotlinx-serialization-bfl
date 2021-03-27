package com.ing.serialization.bfl.serde.element

import com.ing.serialization.bfl.serde.SerdeError
import kotlinx.serialization.ExperimentalSerializationApi
import java.io.DataInput
import java.io.DataOutput

/**
 * The basic abstraction of each object being serialized.
 */
@ExperimentalSerializationApi
abstract class Element(val name: String, val inner: List<Element> = listOf()) {
    abstract val isNullable: Boolean

    protected abstract val inherentLayout: List<Pair<String, Int>>
    private val inherentSize by lazy {
        inherentLayout.sumBy { it.second }
    }

    private val nullLayout by lazy {
        if (isNullable) {
            listOf(Pair("nonNull", 1))
        } else {
            listOf()
        }
    }

    val layout: Layout by lazy {
        Layout(name, nullLayout + inherentLayout, inner.map { it.layout })
    }

    val size by lazy {
        layout.mask.sumBy { it.second }
    }

    abstract fun encodeNull(output: DataOutput)
    fun decodeNull(input: DataInput) {
        input.skipBytes(inherentSize)
    }

    inline fun <reified T : Element> expect(): T {
        // Non-null assertion is fine because T is bound to Element.
        (this as? T) ?: throw SerdeError.UnexpectedElement(T::class.simpleName!!, this)
        return this
    }
}
