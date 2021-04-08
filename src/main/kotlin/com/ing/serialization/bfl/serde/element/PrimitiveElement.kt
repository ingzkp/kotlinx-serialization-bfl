package com.ing.serialization.bfl.serde.element

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.reified.deserialize
import com.ing.serialization.bfl.serde.SerdeError
import com.ing.serialization.bfl.serde.isTrulyPrimitive
import com.ing.serialization.bfl.serializers.DoubleSurrogate
import com.ing.serialization.bfl.serializers.DoubleSurrogate.Companion.DOUBLE_SIZE
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialKind
import java.io.DataInput
import java.io.DataOutput
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties
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
            is PrimitiveKind.FLOAT -> TODO()
//            is PrimitiveKind.DOUBLE -> DOUBLE_SIZE
            is PrimitiveKind.DOUBLE -> serializedSizeOf(DoubleSurrogate::class)
            else -> error("Do not know how to compute layout for primitive $kind")
        }

        listOf(Pair("value", size))
    }

    private fun serializedSizeOf(kClass: KClass<*>): Int {
        if (!kClass.hasAnnotation<Serializable>()) error("Can't determine serializable size of unserializable type. Please annotate with @Serializable.")

        kClass.memberProperties.filter { it.visibility == KVisibility.PUBLIC }.sumBy { serializedSizeOf(it) }
    }

    private fun serializedSizeOf(kProperty1: KProperty1<out Any, *>): Int {
        return if (!kProperty1.hasAnnotation<FixedLength>()) {

        } else {

        }


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
                kind is PrimitiveKind.FLOAT && value is Float -> TODO()
                kind is PrimitiveKind.DOUBLE && value is Double -> writeDouble(stream, value)
                else -> error("$serialName cannot encode $value of type ${value::class.simpleName}")
            }
        }
    }

    private fun writeFloat(stream: DataOutput, value: Float?) {
        val serialization = when (value) {
            is Float -> inlinedSerialize(FloatSurrogate.from(value))
            else -> ByteArray(FLOAT_SIZE) { 0 }
        }
        stream.write(serialization)
    }

    private fun writeDouble(stream: DataOutput, value: Double?) {
        val serialization = when (value) {
            is Double -> inlinedSerialize(DoubleSurrogate.from(value))
            else -> ByteArray(DOUBLE_SIZE) { 0 }
        }
        stream.write(serialization)
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
                is PrimitiveKind.FLOAT -> TODO()
                is PrimitiveKind.DOUBLE -> this@PrimitiveElement.writeDouble(this, null)
                else -> error("Don't know how to encode null for primitive $kind.")
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
                is PrimitiveKind.FLOAT -> TODO()
                is PrimitiveKind.DOUBLE -> this@PrimitiveElement.readDouble(this)
                else -> error("Do not know how to decode primitive $kind")
            } as? T ?: error("$serialName cannot decode required type")
        }

    private fun readDouble(input: DataInput): Double {
        val surrogateInput = ByteArray(DOUBLE_SIZE)
        input.readFully(surrogateInput)
        return deserialize<DoubleSurrogate>(surrogateInput).toOriginal()
    }
}
