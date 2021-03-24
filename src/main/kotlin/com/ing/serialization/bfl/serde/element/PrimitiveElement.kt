package com.ing.serialization.bfl.serde.element

import com.ing.serialization.bfl.serde.Element
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialKind
import java.io.DataOutput

/**
 * The basic abstraction of each object being serialized.
 */
@ExperimentalSerializationApi
class PrimitiveElement(name: String, private val kind: SerialKind, override val isNullable: Boolean) : Element(name) {
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

    @Suppress("ComplexMethod")
    fun encode(stream: DataOutput, value: Any) {
        with(stream) {
            when {
                kind is PrimitiveKind.BOOLEAN && value is Boolean -> writeBoolean(value)
                kind is PrimitiveKind.BYTE && value is Byte -> writeByte(value.toInt())
                kind is PrimitiveKind.SHORT && value is Short -> writeShort(value.toInt())
                kind is PrimitiveKind.INT && value is Int -> writeInt(value)
                kind is PrimitiveKind.LONG && value is Long -> writeLong(value)
                kind is PrimitiveKind.FLOAT && value is Float -> throw IllegalStateException("Floats are not yet supported")
                kind is PrimitiveKind.DOUBLE && value is Double -> throw IllegalStateException("Double are not yet supported")
                kind is PrimitiveKind.CHAR && value is Char -> writeChar('\u0000'.toInt())
                else -> throw IllegalStateException("$name cannot encode $value of type ${value::class.simpleName}")
            }
        }
    }

    override fun encodeNull(stream: DataOutput) =
        with(stream) {
            when (kind) {
                is PrimitiveKind.BOOLEAN -> writeBoolean(false)
                is PrimitiveKind.BYTE -> writeByte(0)
                is PrimitiveKind.SHORT -> writeShort(0)
                is PrimitiveKind.INT -> writeInt(0)
                is PrimitiveKind.LONG -> writeLong(0)
                is PrimitiveKind.FLOAT -> throw IllegalStateException("Floats are not yet supported")
                is PrimitiveKind.DOUBLE -> throw IllegalStateException("Double are not yet supported")
                is PrimitiveKind.CHAR -> writeChar('\u0000'.toInt())
                else -> throw IllegalStateException("$name is called primitive while it is not")
            }
        }
}
