package com.ing.serialization.bfl.serde.serializers.custom.polymorphic

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.serialization.bfl.api.serialize
import io.kotest.matchers.shouldBe
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalStateException

// =========================================== Polymorphic GrandParent =========================================== //

interface PolyGrandParent
data class VariantGrandParentA(val myChild: PolyParent) : PolyGrandParent
data class VariantGrandParentB(val myChild: PolyParent) : PolyGrandParent

object VariantGrandParentASerializer : KSerializer<VariantGrandParentA>
by (SurrogateSerializer(VariantGrandParentASurrogate.serializer()) { VariantGrandParentASurrogate(it.myChild) })

@Serializable
data class VariantGrandParentASurrogate(
    @SerialName("myChild")
    val value: PolyParent
) : Surrogate<VariantGrandParentA> {
    override fun toOriginal() = VariantGrandParentA(value)
}

object VariantGrandParentBSerializer : KSerializer<VariantGrandParentB>
by (SurrogateSerializer(VariantGrandParentBSurrogate.serializer()) { VariantGrandParentBSurrogate(it.myChild) })

@Serializable
data class VariantGrandParentBSurrogate(
    @SerialName("myChild")
    val value: PolyParent
) : Surrogate<VariantGrandParentB> {
    override fun toOriginal() = VariantGrandParentB(value)
}

// =========================================== Polymorphic Parent =========================================== //

interface PolyParent
data class VariantParentA(val myChild: PolyChild) : PolyParent
data class VariantParentB(val myChild: PolyChild) : PolyParent

object VariantParentASerializer : KSerializer<VariantParentA>
by (SurrogateSerializer(VariantParentASurrogate.serializer()) { VariantParentASurrogate(it.myChild) })

@Serializable
data class VariantParentASurrogate(
    @SerialName("myChild")
    val value: PolyChild
) : Surrogate<VariantParentA> {
    override fun toOriginal() = VariantParentA(value)
}

object VariantParentBSerializer : KSerializer<VariantParentB>
by (SurrogateSerializer(VariantParentBSurrogate.serializer()) { VariantParentBSurrogate(it.myChild) })

@Serializable
data class VariantParentBSurrogate(
    @SerialName("myChild")
    val value: PolyChild
) : Surrogate<VariantParentB> {
    override fun toOriginal() = VariantParentB(value)
}

// =========================================== Polymorphic Child =========================================== //

interface PolyChild
data class VariantChildA(val myInt: Int) : PolyChild
data class VariantChildB(val myLong: Long) : PolyChild

object VariantChildASerializer : KSerializer<VariantChildA>
by (SurrogateSerializer(VariantChildASurrogate.serializer()) { VariantChildASurrogate(it.myInt) })

@Serializable
data class VariantChildASurrogate(
    @SerialName("myInt")
    val value: Int
) : Surrogate<VariantChildA> {
    override fun toOriginal() = VariantChildA(value)
}

object VariantChildBSerializer : KSerializer<VariantChildB>
by (SurrogateSerializer(VariantChildBSurrogate.serializer()) { VariantChildBSurrogate(it.myLong) })

@Serializable
data class VariantChildBSurrogate(
    @SerialName("myLong")
    val value: Long
) : Surrogate<VariantChildB> {
    override fun toOriginal() = VariantChildB(value)
}

class DeepPolymorphicTest {
    @Serializable
    data class Data(@FixedLength([2]) val myList: List<PolyParent>)

    @Serializable
    data class GrandParentData(@FixedLength([2]) val myList: List<PolyGrandParent>)

    @Serializable
    data class ManyData(@FixedLength([2, 2]) val myList1: List<PolyParent>, val myList2: List<PolyParent>)

    @Serializable
    data class NestedData(val myData: Data)

    @Serializable
    data class NestedListData(@FixedLength([2]) val myDataList: List<Data>)

    private val nestedPolySerializers = SerializersModule {
        polymorphic(PolyGrandParent::class) {
            subclass(VariantGrandParentA::class, VariantGrandParentASerializer)
            subclass(VariantGrandParentB::class, VariantGrandParentBSerializer)
        }
        contextual(VariantGrandParentASerializer)
        contextual(VariantGrandParentBSerializer)

        polymorphic(PolyParent::class) {
            subclass(VariantParentA::class, VariantParentASerializer)
            subclass(VariantParentB::class, VariantParentBSerializer)
        }
        contextual(VariantParentASerializer)
        contextual(VariantParentBSerializer)

        polymorphic(PolyChild::class) {
            subclass(VariantChildA::class, VariantChildASerializer)
            subclass(VariantChildB::class, VariantChildBSerializer)
        }
        contextual(VariantChildASerializer)
        contextual(VariantChildBSerializer)
    }

    @Test
    fun `different variants of a first-level nested polymorphic type should not coexist in a collection`() {
        val data1 = VariantParentA(VariantChildA(1))
        val data2 = VariantParentA(VariantChildA(2))
        val data3 = VariantParentB(VariantChildA(1))

        listOf(
            Data(listOf(data1, data2)),
            ManyData(listOf(data1, data2), listOf(data3)),
            NestedData(Data(listOf(data1, data2))),
            NestedListData(listOf(Data(listOf(data1, data2)), Data(listOf(data1)))),
            GrandParentData(listOf(VariantGrandParentA(data1), VariantGrandParentA(data2))),
        ).forEach {
            assertDoesNotThrow {
                serialize(it, serializersModule = nestedPolySerializers)
            }
        }

        listOf(
            Data(listOf(data1, data3)),
            ManyData(listOf(data1), listOf(data2, data3)),
            NestedData(Data(listOf(data2, data3))),
            NestedListData(listOf(Data(listOf(data1, data2)), Data(listOf(data3)))),
            GrandParentData(listOf(VariantGrandParentA(data1), VariantGrandParentB(data1))),
        ).forEach {
            assertThrows<IllegalStateException> {
                serialize(it, serializersModule = nestedPolySerializers)
            }.also { exception ->
                exception.message shouldBe "Different implementations of the same base type are not allowed"
            }
        }
    }

    @Test
    fun `different variants of a second-level nested polymorphic type should not coexist in a collection`() {
        val data1 = VariantParentA(VariantChildA(1))
        val data2 = VariantParentA(VariantChildB(1L))
        val data3 = VariantParentB(VariantChildA(1))

        listOf(
            Data(listOf(data1, data2)),
            ManyData(listOf(data1), listOf(data1, data2)),
            NestedData(Data(listOf(data1, data2))),
            NestedListData(listOf(Data(listOf(data1)), Data(listOf(data2)))),
            GrandParentData(listOf(VariantGrandParentA(data1), VariantGrandParentA(data3))),
        ).forEach {
            assertThrows<IllegalStateException> {
                serialize(it, serializersModule = nestedPolySerializers)
            }.also { exception ->
                exception.message shouldBe "Different implementations of the same base type are not allowed"
            }
        }
    }

    @Test
    fun `different variants of a third-level nested polymorphic type should not coexist in a collection`() {
        val data1 = VariantParentA(VariantChildA(1))
        val data2 = VariantParentA(VariantChildB(1L))

        assertThrows<IllegalStateException> {
            serialize(
                GrandParentData(listOf(VariantGrandParentA(data1), VariantGrandParentA(data2))),
                serializersModule = nestedPolySerializers
            )
        }.also { exception ->
            exception.message shouldBe "Different implementations of the same base type are not allowed"
        }
    }
}
