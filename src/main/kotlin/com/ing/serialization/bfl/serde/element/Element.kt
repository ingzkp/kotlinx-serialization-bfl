package com.ing.serialization.bfl.serde.element

import com.ing.serialization.bfl.serde.SerdeError
import java.io.DataInput
import java.io.DataOutput

/**
 * The basic abstraction of each object being serialized.
 */

abstract class Element(val serialName: String, val propertyName: String, var inner: MutableList<Element> = mutableListOf()) {
    abstract var isNullable: Boolean
    var parent: Element? = null
    var isNull: Boolean = false

    protected abstract val inherentLayout: List<Pair<String, Int>>
    private val inherentSize by lazy {
        inherentLayout.sumBy { it.second }
    }

    private val nullLayout by lazy {
        if (isNullable) {
            listOf(Pair("[nullability flag]", 1))
        } else {
            listOf()
        }
    }

    val layout: Layout by lazy {
        Layout(propertyName, serialName, nullLayout + inherentLayout, inner.map { it.layout })
    }

    val size by lazy {
        layout.mask.sumBy { it.second }
    }

    abstract fun encodeNull(output: DataOutput)
    open fun decodeNull(input: DataInput) {
        input.skipBytes(inherentSize)
    }

    inline fun <reified T : Element> expect(): T {
        // Non-null assertion is fine because T is bound to Element.
        (this as? T) ?: throw SerdeError.UnexpectedElement(T::class.simpleName!!, this)
        return this
    }

    open fun verifySelfResolvabilityOrThrow() = Unit

    fun verifyResolvabilityOrThrow(): Element {
        // first verify resolvability of the element itself
        verifySelfResolvabilityOrThrow()

        // then check also the children of the element for resolvability
        inner.forEach { it.verifyResolvabilityOrThrow() }
        return this
    }

    abstract fun clone(): Element
}
