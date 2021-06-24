package com.ing.serialization.bfl.serde.serializers.doc

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class NullableWithinNullableTest {
    @Serializable
    data class ThirdLevelData(val thirdLevelValue: Int? = null)

    @Serializable
    data class SecondLevelData(val secondLevelValue: ThirdLevelData? = null)

    @Serializable
    data class FirstLevelData(val firstLevelValue: SecondLevelData? = null)

    @Serializable
    data class ListData(@FixedLength([2]) val listData: List<FirstLevelData?>? = null)

    @Test
    fun `data with 2 levels of nullable nesting should be serialized successfully`() {
        val mask = listOf(
            Pair("secondLevelValue.isNull", 1),
            Pair("secondLevelValue.thirdLevelValue.isNull", 1),
            Pair("secondLevelValue.thirdLevelValue.value", 4)
        )

        val data = SecondLevelData(ThirdLevelData())
        checkedSerializeInlined(data, mask)
        checkedSerialize(data, mask)
    }

    @Test
    fun `data with 2 levels of nullable nesting should be the same after serialization and deserialization`() {
        val data = SecondLevelData(ThirdLevelData())

        roundTripInlined(data)
        roundTrip(data)
    }

    @Test
    fun `different data with 2 levels of nullable nesting should have same size after serialization`() {
        val data1 = SecondLevelData(ThirdLevelData())
        val data2 = SecondLevelData(ThirdLevelData(1))

        sameSize(data2, data1)
        sameSizeInlined(data2, data1)
    }

    @Test
    fun `data with 3 levels of nullable nesting should be serialized successfully`() {
        val mask = listOf(
            Pair("firstLevelValue.isNull", 1),
            Pair("firstLevelValue.secondLevelValue.isNull", 1),
            Pair("firstLevelValue.secondLevelValue.thirdLevelValue.isNull", 1),
            Pair("firstLevelValue.secondLevelValue.thirdLevelValue.value", 4)
        )

        val data = FirstLevelData(SecondLevelData())
        checkedSerializeInlined(data, mask)
        checkedSerialize(data, mask)
    }

    @Test
    fun `data with 3 levels of nullable nesting should be the same after serialization and deserialization`() {
        val data = FirstLevelData(SecondLevelData())

        roundTripInlined(data)
        roundTrip(data)
    }

    @Test
    fun `different data with 3 levels of nullable nesting should have same size after serialization`() {
        val data1 = FirstLevelData(SecondLevelData())
        val data2 = FirstLevelData(SecondLevelData(ThirdLevelData()))
        val data3 = FirstLevelData(SecondLevelData(ThirdLevelData(1)))

        sameSize(data2, data1)
        sameSizeInlined(data2, data1)

        sameSize(data3, data1)
        sameSizeInlined(data3, data1)
    }

    @Test
    fun `data with list of 3 levels of nullable nesting should be serialized successfully`() {
        val mask = listOf(
            Pair("listData.isNull", 1),
            Pair("listData.length", 4),
            Pair("listData[0].isNull", 1),
            Pair("listData[0].firstLevelValue.isNull", 1),
            Pair("listData[0].firstLevelValue.secondLevelValue.isNull", 1),
            Pair("listData[0].firstLevelValue.secondLevelValue.thirdLevelValue.isNull", 1),
            Pair("listData[0].firstLevelValue.secondLevelValue.thirdLevelValue.value", 4),
            Pair("listData[1].isNull", 1),
            Pair("listData[1].firstLevelValue.isNull", 1),
            Pair("listData[1].firstLevelValue.secondLevelValue.isNull", 1),
            Pair("listData[1].firstLevelValue.secondLevelValue.thirdLevelValue.isNull", 1),
            Pair("listData[1].firstLevelValue.secondLevelValue.thirdLevelValue.value", 4),
        )

        val data = ListData()
        checkedSerializeInlined(data, mask)
        checkedSerialize(data, mask)
    }

    @Test
    fun `data with list of 3 levels of nullable nesting should be the same after serialization and deserialization`() {
        val data = ListData()

        roundTripInlined(data)
        roundTrip(data)
    }

    @ParameterizedTest
    @MethodSource("testData")
    fun `different data with list of 3 levels of nullable nesting should have same size after serialization`(dataPair: Pair<ListData, ListData>) {
        val data1 = dataPair.first
        val data2 = dataPair.second

        sameSize(data2, data1)
        sameSizeInlined(data2, data1)
    }

    companion object {
        @JvmStatic
        fun testData() = listOf(
            Pair(ListData(), ListData(listOf())),
            Pair(ListData(), ListData(listOf(FirstLevelData()))),
            Pair(ListData(), ListData(listOf(FirstLevelData(SecondLevelData())))),
            Pair(ListData(), ListData(listOf(FirstLevelData(SecondLevelData(ThirdLevelData()))))),
            Pair(ListData(), ListData(listOf(FirstLevelData(SecondLevelData(ThirdLevelData(1)))))),
        )
    }
}
