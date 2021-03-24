package com.ing.serialization.bfl.serde.element

import com.ing.serialization.bfl.serde.Element
import com.ing.serialization.bfl.serde.SerdeError
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialKind
import java.io.DataInput
import java.io.DataOutput

/**
 * The basic abstraction of each object being serialized.
 */
@ExperimentalSerializationApi
class PrimitiveElement(name: String, private val kind: SerialKind, override val isNullable: Boolean) : Element(name) {
    init {
        when (kind) {
            is PrimitiveKind.BOOLEAN,
            PrimitiveKind.BYTE,
            PrimitiveKind.SHORT,
            PrimitiveKind.INT,
            PrimitiveKind.LONG,
            PrimitiveKind.CHAR -> { /* OK */ }
            // Unsupported types in Zinc.
            PrimitiveKind.FLOAT,
            PrimitiveKind.DOUBLE -> throw SerdeError.UnsupportedPrimitive(kind)
            // Non-Primitive types.
            else -> throw SerdeError.NonPrimitive(kind)
        }
    }

    override val layout by lazy {
        val size = when (kind) {
            is PrimitiveKind.BOOLEAN -> 1
            is PrimitiveKind.BYTE -> 1
            is PrimitiveKind.SHORT -> 2
            is PrimitiveKind.INT -> 4
            is PrimitiveKind.LONG -> 8
            is PrimitiveKind.CHAR -> 2
            is PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> throw SerdeError.UnsupportedPrimitive(kind)
            else -> throw SerdeError.Unreachable
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
                kind is PrimitiveKind.CHAR && value is Char -> writeChar('\u0000'.toInt())
                (kind is PrimitiveKind.FLOAT) || (kind is PrimitiveKind.DOUBLE) -> throw SerdeError.UnsupportedPrimitive(kind)
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
                is PrimitiveKind.CHAR -> writeChar('\u0000'.toInt())
                is PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> throw SerdeError.UnsupportedPrimitive(kind)
                else -> throw SerdeError.Unreachable
            }
        }

    @Suppress("UNCHECKED_CAST")
    fun <T> decode(stream: DataInput): T =
        with(stream) {
            when (kind) {
                is PrimitiveKind.BOOLEAN -> readBoolean()
                is PrimitiveKind.BYTE -> readByte()
                is PrimitiveKind.SHORT -> readShort()
                is PrimitiveKind.INT -> readInt()
                is PrimitiveKind.LONG -> readLong()
                is PrimitiveKind.CHAR -> readChar()
                is PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> throw SerdeError.UnsupportedPrimitive(kind)
                else -> throw SerdeError.Unreachable
            } as? T ?: throw IllegalStateException("$name cannot decode required type")
        }
}
