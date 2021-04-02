package com.ing.serialization.bfl.serde.element

import com.ing.serialization.bfl.api.reified.deserialize
import com.ing.serialization.bfl.serde.SerdeError
import com.ing.serialization.bfl.serde.isTrulyPrimitive
import com.ing.serialization.bfl.serializers.DoubleSurrogate
import com.ing.serialization.bfl.serializers.DoubleSurrogate.Companion.DOUBLE_SIZE
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialKind
import java.io.DataInput
import java.io.DataOutput
import java.math.BigDecimal
import com.ing.serialization.bfl.api.reified.serialize as inlinedSerialize

/**
 * The basic abstraction of each object being serialized.
 */

class PrimitiveElement(
    serialName: String,
    propertyName: String,
    private val kind: SerialKind,
    override val isNullable: Boolean
) : Element(serialName, propertyName) {
    init {
        if (!kind.isTrulyPrimitive) throw SerdeError.NotFixedPrimitive(kind)
    }

    override val inherentLayout by lazy {
        val size = when (kind) {
            is PrimitiveKind.BOOLEAN -> 1
            is PrimitiveKind.BYTE -> 1
            is PrimitiveKind.SHORT -> 2
            is PrimitiveKind.INT -> 4
            is PrimitiveKind.LONG -> 8
            is PrimitiveKind.CHAR -> 2
            is PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> BigDecimalSurrogate.DOUBLE_SIZE
            else -> error("Do not know how to compute layout for primitive $kind")
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
                kind is PrimitiveKind.CHAR && value is Char -> writeChar(value.toInt())
                kind is PrimitiveKind.FLOAT && value is Float -> {
                    val surrogate = DoubleSurrogate.from(value)
                    writeBigDecimal(stream, surrogate)
                }
                kind is PrimitiveKind.DOUBLE && value is Double -> {
                    val surrogate = DoubleSurrogate.from(value)
                    writeBigDecimal(stream, surrogate)
                }
                else -> error("$serialName cannot encode $value of type ${value::class.simpleName}")
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
                is PrimitiveKind.CHAR -> writeChar(0)
                is PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE -> writeBigDecimal(this, null)
                else -> error("Encoding null for primitive $kind.")
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
                else -> error("Do not know how to decode primitive $kind")
            } as? T ?: error("$serialName cannot decode required type")
        }

    private fun writeBigDecimal(output: DataOutput, surrogate: DoubleSurrogate?) {
        val serialization = surrogate?.let { inlinedSerialize(surrogate) }
            ?: ByteArray(DOUBLE_SIZE) { 0 }
        output.write(serialization)
    }

    private fun readBigDecimal(input: DataInput): BigDecimal {
        val surrogateInput = ByteArray(DOUBLE_SIZE)
        input.readFully(surrogateInput)

        val surrogate = deserialize<DoubleSurrogate>(surrogateInput)

        return surrogate.toOriginal()
    }
}
