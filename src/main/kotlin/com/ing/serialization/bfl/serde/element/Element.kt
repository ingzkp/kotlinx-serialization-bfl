package com.ing.serialization.bfl.serde

import com.ing.serialization.bfl.serde.element.Layout
import kotlinx.serialization.ExperimentalSerializationApi
import java.io.DataOutput

/**
 * The basic abstraction of each object being serialized.
 */
@ExperimentalSerializationApi
abstract class Element(val name: String) {
    abstract val layout: Layout

    open val size by lazy {
        layout.mask.sumBy { it.second }
    }

    abstract val isNullable: Boolean
    val nullLayout by lazy {
        if (isNullable) {
            listOf(Pair("nonNull", 1))
        } else {
            listOf()
        }
    }

    abstract fun encodeNull(stream: DataOutput)

    inline fun <reified T : Element> expect(): T {
        // Non-null assertion is fine because T is bound to Element.
        (this as? T) ?: throw SerdeError.WrongElement(T::class.simpleName!!, this)
        return this
    }
}
