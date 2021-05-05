package com.ing.serialization.bfl.serde.serializers.custom.polymorphic

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.serialization.bfl.api.debugSerialize
import com.ing.serialization.bfl.api.serialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.element.ElementFactory
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import io.kotest.matchers.string.shouldMatch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

// =========================================== Polymorphic Base =========================================== //

interface PolyBase
data class VariantA(val myInt: Int) : PolyBase
data class VariantB(val myLong: Long) : PolyBase
data class VariantC(val myByte: Byte) : PolyBase

object VariantASerializer : KSerializer<VariantA>
by (SurrogateSerializer(VariantASurrogate.serializer()) { VariantASurrogate(it.myInt) })

@Serializable
data class VariantASurrogate(
    @SerialName("myInt")
    val value: Int
) : Surrogate<VariantA> {
    constructor(variantA: VariantA) : this(variantA.myInt)
    override fun toOriginal() = VariantA(value)
}

object VariantBSerializer : KSerializer<VariantB>
by (SurrogateSerializer(VariantBSurrogate.serializer()) { VariantBSurrogate(it.myLong) })

@Serializable
data class VariantBSurrogate(
    @SerialName("myLong")
    val value: Long
) : Surrogate<VariantB> {
    override fun toOriginal() = VariantB(value)
}

object VariantCSerializer : KSerializer<VariantC>
by (SurrogateSerializer(VariantCSurrogate.serializer()) { VariantCSurrogate(it.myByte) })

@Serializable
data class VariantCSurrogate(
    @SerialName("myByte")
    val value: Byte
) : Surrogate<VariantC> {
    override fun toOriginal() = VariantC(value)
}

class NullablePolymorphicTest {
    @Serializable
    data class Data(@FixedLength([3]) val myList: List<PolyBase?>)

    @Serializable
    data class ComplexData(val myData: PolyBase?, @FixedLength([3]) val myList: List<PolyBase?>)

    @Serializable
    data class ComplexDataList(@FixedLength([3]) val dataList: List<ComplexData?>)

    private val nullablePolySerializers = SerializersModule {
        polymorphic(PolyBase::class) {
            subclass(VariantA::class, VariantASerializer)
            subclass(VariantB::class, VariantBSerializer)
            subclass(VariantC::class, VariantCSerializer)
        }
        contextual(VariantASerializer)
        contextual(VariantBSerializer)
        contextual(VariantCSerializer)
    }

    @Test
    fun `list with at least one non-null nullable polymorphic should be serialized successfully`() {
        val data = Data(listOf(null, VariantA(0), null))

        assertDoesNotThrow {
            debugSerialize(data, serializersModule = nullablePolySerializers)
        }.also {
            println(it.second)
        }
    }

    @Test
    fun `list with at least one non-null nullable polymorphic should be the same after serialization and deserialization`() {
        val data = Data(listOf(null, VariantA(0), null))

        roundTripInlined(data, nullablePolySerializers)
        roundTrip(data, nullablePolySerializers)
    }

    @Test
    fun `different lists with at least one non-null nullable polymorphic should have same size after serialization`() {
        val data1 = Data(listOf(null, VariantA(0), null))
        val data2 = Data(listOf(VariantA(0), VariantA(1)))

        sameSizeInlined(data1, data2, nullablePolySerializers)
        sameSize(data1, data2, nullablePolySerializers)
    }

    @Test
    fun `inner list with at least one non-null nullable polymorphic should be serialized successfully`() {
        val data = ComplexData(VariantB(0), listOf(null, VariantA(0), null))

        assertDoesNotThrow {
            debugSerialize(data, serializersModule = nullablePolySerializers)
        }.also {
            println(it.second)
        }
    }

    @Test
    fun `inner list with at least one non-null nullable polymorphic should be the same after serialization and deserialization`() {
        val data = ComplexData(VariantB(0), listOf(null, VariantA(0), null))

        roundTripInlined(data, nullablePolySerializers)
        roundTrip(data, nullablePolySerializers)
    }

    @Test
    fun `different inner lists with at least one non-null nullable polymorphic should have same size after serialization`() {
        val data1 = ComplexData(VariantB(0), listOf(null, VariantA(0), null))
        val data2 = ComplexData(VariantB(0), listOf(VariantA(0), VariantA(1), VariantA(2)))

        sameSizeInlined(data1, data2, nullablePolySerializers)
        sameSize(data1, data2, nullablePolySerializers)
    }

    @Test
    @Suppress("LongMethod")
    fun `list of nested nullable polymorphic should be serialized successfully when implementation for each inner polymorphic is provided`() {
        val mask = listOf(
            Pair("dataList.length", 4),
            Pair("dataList[0].isNull", 1),
            Pair("dataList[0].myData.isNull", 1),
            Pair("dataList[0].myData.serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("dataList[0].myData.value.myLong", 8),
            Pair("dataList[0].myList.length", 4),
            Pair("dataList[0].myList[0].isNull", 1),
            Pair("dataList[0].myList[0].serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("dataList[0].myList[0].value.myInt", 4),
            Pair("dataList[0].myList[1].isNull", 1),
            Pair("dataList[0].myList[1].serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("dataList[0].myList[1].value.myInt", 4),
            Pair("dataList[0].myList[2].isNull", 1),
            Pair("dataList[0].myList[2].serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("dataList[0].myList[2].value.myInt", 4),
            Pair("dataList[1].isNull", 1),
            Pair("dataList[1].myData.isNull", 1),
            Pair("dataList[1].myData.serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("dataList[1].myData.value.myLong", 8),
            Pair("dataList[1].myList.length", 4),
            Pair("dataList[1].myList[0].isNull", 1),
            Pair("dataList[1].myList[0].serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("dataList[1].myList[0].value.myInt", 4),
            Pair("dataList[1].myList[1].isNull", 1),
            Pair("dataList[1].myList[1].serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("dataList[1].myList[1].value.myInt", 4),
            Pair("dataList[1].myList[2].isNull", 1),
            Pair("dataList[1].myList[2].serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("dataList[1].myList[2].value.myInt", 4),
            Pair("dataList[2].isNull", 1),
            Pair("dataList[2].myData.isNull", 1),
            Pair("dataList[2].myData.serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("dataList[2].myData.value.myLong", 8),
            Pair("dataList[2].myList.length", 4),
            Pair("dataList[2].myList[0].isNull", 1),
            Pair("dataList[2].myList[0].serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("dataList[2].myList[0].value.myInt", 4),
            Pair("dataList[2].myList[1].isNull", 1),
            Pair("dataList[2].myList[1].serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("dataList[2].myList[1].value.myInt", 4),
            Pair("dataList[2].myList[2].isNull", 1),
            Pair("dataList[2].myList[2].serialName", 2 + 2 * ElementFactory.polySerialNameLength),
            Pair("dataList[2].myList[2].value.myInt", 4),
        )

        val data = ComplexDataList(
            listOf(
                ComplexData(null, listOf(null, null, null)),
                ComplexData(VariantB(0), listOf(null, VariantA(0), null)),
            )
        )

        checkedSerializeInlined(data, mask, nullablePolySerializers)

        assertDoesNotThrow {
            debugSerialize(data, serializersModule = nullablePolySerializers)
        }.also {
            println(it.second)
        }
    }

    @Test
    fun `list of nested nullable polymorphic should be the same after serialization and deserialization`() {
        val data = ComplexDataList(
            listOf(
                ComplexData(null, listOf(null, null, null)),
                ComplexData(VariantB(0), listOf(null, VariantA(0), null)),
            )
        )

        roundTripInlined(data, nullablePolySerializers)
        roundTrip(data, nullablePolySerializers)
    }

    @Test
    fun `different lists of nested nullable polymorphic should have same size after serialization`() {
        val data1 = ComplexDataList(
            listOf(
                ComplexData(null, listOf(null, null, null)),
                ComplexData(VariantB(0), listOf(null, VariantA(0), null)),
            )
        )
        val data2 = ComplexDataList(
            listOf(
                ComplexData(VariantB(1), listOf(null, null, VariantA(0))),
                ComplexData(VariantB(0), listOf(VariantA(0), VariantA(1), null)),
            )
        )

        sameSizeInlined(data1, data2, nullablePolySerializers)
        sameSize(data1, data2, nullablePolySerializers)
    }

    @Test
    fun `Serialization of nullable polymorphic at any nesting level should fail when implementation cannot be inferred`() {
        listOf(
            Data(listOf()),
            ComplexData(null, listOf(VariantA(0))),
            ComplexData(VariantA(0), listOf(null)),
            ComplexDataList(
                listOf(
                    ComplexData(null, listOf(null, null, null)),
                    ComplexData(null, listOf(null, VariantA(0), null)),
                )
            ),
            ComplexDataList(
                listOf(
                    ComplexData(VariantA(0), listOf(null)),
                    ComplexData(null, listOf(null)),
                )
            ),
        ).forEach {
            assertThrows<IllegalStateException> {
                serialize(it, serializersModule = nullablePolySerializers)
            }.also { exception ->
                exception.message shouldMatch Regex("Implementation of '.+' cannot be inferred")
            }
        }
    }
}
