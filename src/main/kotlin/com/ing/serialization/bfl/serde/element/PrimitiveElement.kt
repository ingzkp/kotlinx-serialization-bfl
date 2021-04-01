package com.ing.serialization.bfl.serde.element

import com.ing.serialization.bfl.serde.SerdeError
import com.ing.serialization.bfl.serializers.BigDecimalSurrogate
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialKind
import java.io.DataInput
import java.io.DataOutput
import java.math.BigDecimal
import com.ing.serialization.bfl.api.reified.deserialize as deserializeInlined
import com.ing.serialization.bfl.api.reified.serialize as serializeInlined

/**
 * The basic abstraction of each object being serialized.
 */

class PrimitiveElement(name: String, private val kind: SerialKind, override val isNullable: Boolean) : Element(name) {
    init {
        when (kind) {
            is PrimitiveKind.BOOLEAN,
            PrimitiveKind.BYTE,
            PrimitiveKind.SHORT,
            PrimitiveKind.INT,
            PrimitiveKind.LONG,
            PrimitiveKind.CHAR,
            PrimitiveKind.FLOAT,
            PrimitiveKind.DOUBLE -> { /* OK */ }
            // Non-Primitive types.
            else -> throw SerdeError.NonPrimitive(kind)
        }
    }

    override val inherentLayout by lazy {
        val size = when (kind) {
            is PrimitiveKind.BOOLEAN -> 1
            is PrimitiveKind.BYTE -> 1
            is PrimitiveKind.SHORT -> 2
            is PrimitiveKind.INT -> 4
            is PrimitiveKind.LONG -> 8
            is PrimitiveKind.CHAR -> 2
            is PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> BigDecimalSurrogate.SIZE
            else -> throw SerdeError.Unreachable("Computing layout for primitive $kind")
        }

        listOf(Pair("value", size))
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
                kind is PrimitiveKind.FLOAT && value is Float -> {
                    val surrogate = BigDecimalSurrogate.from(value)
                    writeBigDecimal(stream, surrogate)
                }
                kind is PrimitiveKind.DOUBLE && value is Double -> {
                    val surrogate = BigDecimalSurrogate.from(value)
                    writeBigDecimal(stream, surrogate)
                }
                else -> throw IllegalStateException("$name cannot encode $value of type ${value::class.simpleName}")
            }
        }
    }

    override fun encodeNull(output: DataOutput) =
        with(output) {
            when (kind) {
                is PrimitiveKind.BOOLEAN -> writeBoolean(false)
                is PrimitiveKind.BYTE -> writeByte(0)
                is PrimitiveKind.SHORT -> writeShort(0)
                is PrimitiveKind.INT -> writeInt(0)
                is PrimitiveKind.LONG -> writeLong(0)
                is PrimitiveKind.CHAR -> writeChar('\u0000'.toInt())
                is PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> writeBigDecimal(this, null)
                else -> throw SerdeError.Unreachable("Encoding null for primitive $kind.")
            }
        }

    @Suppress("UNCHECKED_CAST")
    fun <T> decode(output: DataInput): T =
        with(output) {
            when (kind) {
                is PrimitiveKind.BOOLEAN -> readBoolean()
                is PrimitiveKind.BYTE -> readByte()
                is PrimitiveKind.SHORT -> readShort()
                is PrimitiveKind.INT -> readInt()
                is PrimitiveKind.LONG -> readLong()
                is PrimitiveKind.CHAR -> readChar()
                is PrimitiveKind.FLOAT -> readBigDecimal(this).toFloat()
                is PrimitiveKind.DOUBLE -> readBigDecimal(this).toDouble()
                else -> throw SerdeError.Unreachable("Decoding a primitive $kind")
            } as? T ?: throw IllegalStateException("$name cannot decode required type")
        }

    private fun writeBigDecimal(output: DataOutput, surrogate: BigDecimalSurrogate?) {
        val serialization = surrogate?.let { serializeInlined(surrogate) }
            ?: ByteArray(BigDecimalSurrogate.SIZE) { 0 }
        output.write(serialization)
    }

    private fun readBigDecimal(input: DataInput): BigDecimal {
        val surrogateInput = ByteArray(BigDecimalSurrogate.SIZE)
        input.readFully(surrogateInput)

        val surrogate = deserializeInlined<BigDecimalSurrogate>(surrogateInput)

        return surrogate.toOriginal()
    }
}
