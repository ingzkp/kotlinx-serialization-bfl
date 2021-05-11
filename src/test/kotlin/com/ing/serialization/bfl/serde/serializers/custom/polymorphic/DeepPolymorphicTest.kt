package com.ing.serialization.bfl.serde.serializers.custom.polymorphic

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.serialization.bfl.api.serialize
import com.ing.serialization.bfl.serde.SerdeError
import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

// =========================================== Polymorphic Child =========================================== //

interface PolyChild
data class VariantChildA(val myInt: Int) : PolyChild
data class VariantChildB(val myLong: Long) : PolyChild

object VariantChildASerializer :
    SurrogateSerializer<VariantChildA, VariantChildASurrogate>(
        VariantChildASurrogate.serializer(),
        { VariantChildASurrogate(it.myInt) }
    )

@Serializable
@SerialName("CHA")
data class VariantChildASurrogate(
    val value: Int
) : Surrogate<VariantChildA> {
    override fun toOriginal() = VariantChildA(value)
    companion object {
        const val SERIAL_NAME_LENGTH = 2 + 2 * 3
    }
}

object VariantChildBSerializer :
    SurrogateSerializer<VariantChildB, VariantChildBSurrogate>(
        VariantChildBSurrogate.serializer(),
        { VariantChildBSurrogate(it.myLong) }
    )

@Serializable
@SerialName("CHB")
data class VariantChildBSurrogate(
    val value: Long
) : Surrogate<VariantChildB> {
    override fun toOriginal() = VariantChildB(value)
    companion object {
        const val SERIAL_NAME_LENGTH = 2 + 2 * 3
    }
}

// =========================================== Polymorphic Parent =========================================== //

interface PolyParent
data class VariantParentA(val myChild: PolyChild) : PolyParent
data class VariantParentB(val myChild: PolyChild) : PolyParent

object VariantParentASerializer :
    SurrogateSerializer<VariantParentA, VariantParentASurrogate>(
        VariantParentASurrogate.serializer(),
        { VariantParentASurrogate(it.myChild) }
    )

@Serializable
@SerialName("PA")
data class VariantParentASurrogate(
    val value: PolyChild
) : Surrogate<VariantParentA> {
    override fun toOriginal() = VariantParentA(value)
    companion object {
        const val SERIAL_NAME_LENGTH = 2 + 2 * 2
    }
}

object VariantParentBSerializer :
    SurrogateSerializer<VariantParentB, VariantParentBSurrogate>(
        VariantParentBSurrogate.serializer(),
        { VariantParentBSurrogate(it.myChild) }
    )

@Serializable
@SerialName("PB")
data class VariantParentBSurrogate(
    val value: PolyChild
) : Surrogate<VariantParentB> {
    override fun toOriginal() = VariantParentB(value)
    companion object {
        const val SERIAL_NAME_LENGTH = 2 + 2 * 2
    }
}

// =========================================== Polymorphic GrandParent =========================================== //

interface PolyGrandParent
data class VariantGrandParentA(val myChild: PolyParent) : PolyGrandParent
data class VariantGrandParentB(val myChild: PolyParent) : PolyGrandParent

object VariantGrandParentASerializer :
    SurrogateSerializer<VariantGrandParentA, VariantGrandParentASurrogate>(
        VariantGrandParentASurrogate.serializer(),
        { VariantGrandParentASurrogate(it.myChild) }
    )

@Serializable
@SerialName("GPA")
data class VariantGrandParentASurrogate(
    val value: PolyParent
) : Surrogate<VariantGrandParentA> {
    override fun toOriginal() = VariantGrandParentA(value)
    companion object {
        const val SERIAL_NAME_LENGTH = 2 + 2 * 3
    }
}

object VariantGrandParentBSerializer :
    SurrogateSerializer<VariantGrandParentB, VariantGrandParentBSurrogate>(
        VariantGrandParentBSurrogate.serializer(),
        { VariantGrandParentBSurrogate(it.myChild) }
    )

@Serializable
@SerialName("GPB")
data class VariantGrandParentBSurrogate(
    val value: PolyParent
) : Surrogate<VariantGrandParentB> {
    override fun toOriginal() = VariantGrandParentB(value)
    companion object {
        const val SERIAL_NAME_LENGTH = 2 + 2 * 3
    }
}

class DeepPolymorphicTest {
    private val nestedPolySerializers = SerializersModule {
        polymorphic(PolyGrandParent::class) {
            subclass(VariantGrandParentA::class, VariantGrandParentASerializer)
            subclass(VariantGrandParentB::class, VariantGrandParentBSerializer)
        }

        polymorphic(PolyParent::class) {
            subclass(VariantParentA::class, VariantParentASerializer)
            subclass(VariantParentB::class, VariantParentBSerializer)
        }

        polymorphic(PolyChild::class) {
            subclass(VariantChildA::class, VariantChildASerializer)
            subclass(VariantChildB::class, VariantChildBSerializer)
        }
    }

    @Serializable
    data class Data(@FixedLength([2]) val myList: List<PolyParent>)

    @Test
    fun `polymorphic nested in list should be serialized successfully`() {
        val mask = listOf(
            Pair("myList.length", 4),
            Pair("myList[0].polyParent.serialName", VariantParentASurrogate.SERIAL_NAME_LENGTH),
            Pair("myList[0].polyParent.polyChild.serialName", VariantChildASurrogate.SERIAL_NAME_LENGTH),
            Pair("myList[0].polyParent.polyChild.value", 4),
            Pair("myList[1].polyParent.serialName", VariantParentASurrogate.SERIAL_NAME_LENGTH),
            Pair("myList[1].polyParent.polyChild.serialName", VariantChildASurrogate.SERIAL_NAME_LENGTH),
            Pair("myList[1].polyParent.polyChild.value", 4),
        )
        val data = Data(listOf(VariantParentA(VariantChildA(1)), VariantParentA(VariantChildA(2))))

        checkedSerializeInlined(data, mask, nestedPolySerializers)
        checkedSerialize(data, mask, nestedPolySerializers)
    }

    @Test
    fun `polymorphic nested in list should be the same after serialization and deserialization`() {
        val data = Data(listOf(VariantParentA(VariantChildA(1)), VariantParentA(VariantChildA(2))))

        roundTripInlined(data, nestedPolySerializers)
        roundTrip(data, nestedPolySerializers)
    }

    @Test
    fun `different polymorphic nested in list should have same size after serialization`() {
        val data1 = Data(listOf(VariantParentA(VariantChildA(1)), VariantParentA(VariantChildA(2))))
        val data2 = Data(listOf(VariantParentA(VariantChildA(1))))

        sameSizeInlined(data1, data2, nestedPolySerializers)
        sameSize(data1, data2, nestedPolySerializers)
    }

    @Serializable
    data class ManyData(@FixedLength([2, 2]) val myList1: List<PolyParent>, val myList2: List<PolyParent>)

    @Test
    fun `polymorphic nested in different lists should be serialized successfully`() {
        val mask = listOf(
            Pair("myList1.length", 4),
            Pair("myList1[0].polyParent.serialName", VariantParentASurrogate.SERIAL_NAME_LENGTH),
            Pair("myList1[0].polyParent.polyChild.serialName", VariantChildASurrogate.SERIAL_NAME_LENGTH),
            Pair("myList1[0].polyParent.polyChild.value", 4),
            Pair("myList1[1].polyParent.serialName", VariantParentASurrogate.SERIAL_NAME_LENGTH),
            Pair("myList1[1].polyParent.polyChild.serialName", VariantChildASurrogate.SERIAL_NAME_LENGTH),
            Pair("myList1[1].polyParent.polyChild.value", 4),
            Pair("myList2.length", 4),
            Pair("myList2[0].polyParent.serialName", VariantParentASurrogate.SERIAL_NAME_LENGTH),
            Pair("myList2[0].polyParent.polyChild.serialName", VariantChildASurrogate.SERIAL_NAME_LENGTH),
            Pair("myList2[0].polyParent.polyChild.value", 4),
            Pair("myList2[1].polyParent.serialName", VariantParentASurrogate.SERIAL_NAME_LENGTH),
            Pair("myList2[1].polyParent.polyChild.serialName", VariantChildASurrogate.SERIAL_NAME_LENGTH),
            Pair("myList2[1].polyParent.polyChild.value", 4),
        )
        val data = ManyData(
            listOf(VariantParentA(VariantChildA(1)), VariantParentA(VariantChildA(2))),
            listOf(VariantParentA(VariantChildA(2)))
        )

        checkedSerialize(data, mask, nestedPolySerializers)
        checkedSerializeInlined(data, mask, nestedPolySerializers)
    }

    @Test
    fun `polymorphic nested in different lists should be the same after serialization and deserialization`() {
        val data = ManyData(
            listOf(VariantParentA(VariantChildA(1)), VariantParentA(VariantChildA(2))),
            listOf(VariantParentA(VariantChildA(2)))
        )

        roundTripInlined(data, nestedPolySerializers)
        roundTrip(data, nestedPolySerializers)
    }

    @Test
    fun `different polymorphic nested in different lists should have same size after serialization`() {
        val data1 = ManyData(
            listOf(VariantParentA(VariantChildA(1)), VariantParentA(VariantChildA(2))),
            listOf(VariantParentA(VariantChildA(2)))
        )
        val data2 = ManyData(
            listOf(VariantParentA(VariantChildA(1))),
            listOf(VariantParentA(VariantChildA(2)))
        )

        sameSizeInlined(data1, data2, nestedPolySerializers)
        sameSize(data1, data2, nestedPolySerializers)
    }

    @Serializable
    data class NestedData(val myData: Data)

    @Test
    fun `polymorphic nested within structure should be serialized successfully`() {
        val mask = listOf(
            Pair("myData.myList.length", 4),
            Pair("myData.myList[0].polyParent.serialName", VariantParentASurrogate.SERIAL_NAME_LENGTH),
            Pair("myData.myList[0].polyParent.polyChild.serialName", VariantChildASurrogate.SERIAL_NAME_LENGTH),
            Pair("myData.myList[0].polyParent.polyChild.value", 4),
            Pair("myData.myList[1].polyParent.serialName", VariantParentASurrogate.SERIAL_NAME_LENGTH),
            Pair("myData.myList[1].polyParent.polyChild.serialName", VariantChildASurrogate.SERIAL_NAME_LENGTH),
            Pair("myData.myList[1].polyParent.polyChild.value", 4),
        )
        val data = NestedData(Data(listOf(VariantParentA(VariantChildA(1)), VariantParentA(VariantChildA(2)))))

        checkedSerializeInlined(data, mask, nestedPolySerializers)
        checkedSerialize(data, mask, nestedPolySerializers)
    }

    @Test
    fun `polymorphic nested within structure should be the same after serialization and deserialization`() {
        val data = NestedData(Data(listOf(VariantParentA(VariantChildA(1)), VariantParentA(VariantChildA(2)))))

        roundTripInlined(data, nestedPolySerializers)
        roundTrip(data, nestedPolySerializers)
    }

    @Test
    fun `different polymorphic nested within structure should have same size after serialization`() {
        val data1 = NestedData(Data(listOf(VariantParentA(VariantChildA(1)), VariantParentA(VariantChildA(2)))))
        val data2 = NestedData(Data(listOf(VariantParentA(VariantChildA(1)))))

        sameSizeInlined(data1, data2, nestedPolySerializers)
        sameSize(data1, data2, nestedPolySerializers)
    }

    @Serializable
    data class NestedListData(@FixedLength([2]) val myDataList: List<Data>)

    @Test
    fun `polymorphic nested in list of structure should be serialized successfully`() {
        val mask = listOf(
            Pair("myDataList.length", 4),
            Pair("myDataList[0].myList.length", 4),
            Pair("myDataList[0].myList[0].polyParent.serialName", VariantParentASurrogate.SERIAL_NAME_LENGTH),
            Pair("myDataList[0].myList[0].polyParent.polyChild.serialName", VariantChildASurrogate.SERIAL_NAME_LENGTH),
            Pair("myDataList[0].myList[0].polyParent.polyChild.value", 4),
            Pair("myDataList[0].myList[1].polyParent.serialName", VariantParentASurrogate.SERIAL_NAME_LENGTH),
            Pair("myDataList[0].myList[1].polyParent.polyChild.serialName", VariantChildASurrogate.SERIAL_NAME_LENGTH),
            Pair("myDataList[0].myList[1].polyParent.polyChild.value", 4),
            Pair("myDataList[1].myList.length", 4),
            Pair("myDataList[1].myList[0].polyParent.serialName", VariantParentASurrogate.SERIAL_NAME_LENGTH),
            Pair("myDataList[1].myList[0].polyParent.polyChild.serialName", VariantChildASurrogate.SERIAL_NAME_LENGTH),
            Pair("myDataList[1].myList[0].polyParent.polyChild.value", 4),
            Pair("myDataList[1].myList[1].polyParent.serialName", VariantParentASurrogate.SERIAL_NAME_LENGTH),
            Pair("myDataList[1].myList[1].polyParent.polyChild.serialName", VariantChildASurrogate.SERIAL_NAME_LENGTH),
            Pair("myDataList[1].myList[1].polyParent.polyChild.value", 4),
        )
        val data = NestedListData(listOf(Data(listOf(VariantParentA(VariantChildA(1))))))

        checkedSerialize(data, mask, nestedPolySerializers)
        checkedSerializeInlined(data, mask, nestedPolySerializers)
    }

    @Test
    fun `polymorphic nested in list of structure should be the same after serialization and deserialization`() {
        val data = NestedListData(listOf(Data(listOf(VariantParentA(VariantChildA(1))))))

        roundTripInlined(data, nestedPolySerializers)
        roundTrip(data, nestedPolySerializers)
    }

    @Test
    fun `different polymorphic nested in list of structure should have same size after serialization`() {
        val data1 = NestedListData(listOf(Data(listOf(VariantParentA(VariantChildA(1))))))
        val data2 = NestedListData(
            listOf(
                Data(listOf(VariantParentA(VariantChildA(1)))),
                Data(listOf(VariantParentA(VariantChildA(2)))),
            )
        )

        sameSizeInlined(data1, data2, nestedPolySerializers)
        sameSize(data1, data2, nestedPolySerializers)
    }

    @Serializable
    data class GrandParentData(@FixedLength([2]) val myList: List<PolyGrandParent>)

    @Test
    fun `third-level nested polymorphic should be serialized successfully`() {
        val mask = listOf(
            Pair("myList.length", 4),
            Pair("myList[0].PolyGrandParent.serialName", VariantGrandParentASurrogate.SERIAL_NAME_LENGTH),
            Pair("myList[0].PolyGrandParent.polyParent.serialName", VariantParentASurrogate.SERIAL_NAME_LENGTH),
            Pair("myList[0].PolyGrandParent.polyParent.polyChild.serialName", VariantChildASurrogate.SERIAL_NAME_LENGTH),
            Pair("myList[0].PolyGrandParent.polyParent.polyChild.value", 4),
            Pair("myList[1].PolyGrandParent.serialName", VariantGrandParentASurrogate.SERIAL_NAME_LENGTH),
            Pair("myList[1].PolyGrandParent.polyParent.serialName", VariantParentASurrogate.SERIAL_NAME_LENGTH),
            Pair("myList[1].PolyGrandParent.polyParent.polyChild.serialName", VariantChildASurrogate.SERIAL_NAME_LENGTH),
            Pair("myList[1].PolyGrandParent.polyParent.polyChild.value", 4),
        )
        val data = GrandParentData(
            listOf(
                VariantGrandParentA(VariantParentA(VariantChildA(1))),
                VariantGrandParentA(VariantParentA(VariantChildA(2)))
            )
        )

        checkedSerializeInlined(data, mask, nestedPolySerializers)
        checkedSerialize(data, mask, nestedPolySerializers)
    }

    @Test
    fun `third-level nested polymorphic  should be the same after serialization and deserialization`() {
        val data = GrandParentData(
            listOf(
                VariantGrandParentA(VariantParentA(VariantChildA(1))),
                VariantGrandParentA(VariantParentA(VariantChildA(2)))
            )
        )

        roundTripInlined(data, nestedPolySerializers)
        roundTrip(data, nestedPolySerializers)
    }

    @Test
    fun `third-level nested polymorphic  in list should have same size after serialization`() {
        val data1 = GrandParentData(
            listOf(
                VariantGrandParentA(VariantParentA(VariantChildA(1))),
                VariantGrandParentA(VariantParentA(VariantChildA(2)))
            )
        )
        val data2 = GrandParentData(
            listOf(
                VariantGrandParentA(VariantParentA(VariantChildA(1))),
            )
        )

        sameSizeInlined(data1, data2, nestedPolySerializers)
        sameSize(data1, data2, nestedPolySerializers)
    }

    @Test
    fun `different variants of a first-level nested polymorphic type should not coexist in a collection`() {
        val data1 = VariantParentA(VariantChildA(1))
        val data2 = VariantParentA(VariantChildA(2))
        val data3 = VariantParentB(VariantChildA(1))

        listOf(
            Data(listOf(data1, data3)),
            ManyData(listOf(data1), listOf(data2, data3)),
            NestedData(Data(listOf(data2, data3))),
            NestedListData(listOf(Data(listOf(data1, data2)), Data(listOf(data3)))),
            GrandParentData(listOf(VariantGrandParentA(data1), VariantGrandParentB(data1))),
        ).forEach {
            assertThrows<SerdeError.DifferentPolymorphicImplementations> {
                serialize(it, serializersModule = nestedPolySerializers)
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
            assertThrows<SerdeError.DifferentPolymorphicImplementations> {
                serialize(it, serializersModule = nestedPolySerializers)
            }
        }
    }

    @Test
    fun `different variants of a third-level nested polymorphic type should not coexist in a collection`() {
        val data1 = VariantParentA(VariantChildA(1))
        val data2 = VariantParentA(VariantChildB(1L))

        assertThrows<SerdeError.DifferentPolymorphicImplementations> {
            serialize(
                GrandParentData(listOf(VariantGrandParentA(data1), VariantGrandParentA(data2))),
                serializersModule = nestedPolySerializers
            )
        }
    }
}
