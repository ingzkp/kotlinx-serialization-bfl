package com.ing.serialization.bfl.serde.serializers.custom.polymorphic

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.serialization.bfl.api.reified.debugSerialize
import com.ing.serialization.bfl.api.reified.serialize
import com.ing.serialization.bfl.serde.SerdeError
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.sameSize
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DeepPolymorphic {
    @Serializable
    data class Data(@FixedLength([2]) val myList: List<Poly>)

    @Serializable
    data class DataA(@FixedLength([2]) val myList: List<@Contextual VariantA>)

    private val failSerializers = SerializersModule {
        polymorphic(Poly::class) {
            subclass(VariantA::class, VariantAFailingSerializer)
            subclass(VariantB::class, VariantBFailingSerializer)
        }
    }

    private val successSerializers = SerializersModule {
        polymorphic(Poly::class) {
            subclass(VariantA::class, VariantASucceedingSerializer)
            subclass(VariantB::class, VariantBSucceedingSerializer)
        }
        contextual(VariantASucceedingSerializer)
        contextual(VariantBSucceedingSerializer)
    }

    @Test
    fun `serialization of polymorphic types using different surrogates must fail`() {
        val data = Data(listOf(VariantA(1), VariantB(2)))

        println(
            assertThrows<SerdeError.UnexpectedPrimitive> {
                serialize(data, serializersModule = failSerializers)
            }
        )
    }

    @Test
    fun `serialization of polymorphic types using same surrogates must not fail`() {
        val data1 = Data(listOf(VariantA(1), VariantB(2)))
        val data2 = Data(listOf(VariantB(2)))

        println(debugSerialize(data1, serializersModule = successSerializers).second)
        roundTrip(data1, successSerializers)
        sameSize(data1, data2, successSerializers)
    }

    @Test
    fun `serialization of exact poly types using same surrogates`() {
        val data1 = DataA(listOf(VariantA(1), VariantA(2)))
        val data2 = DataA(listOf(VariantA(2)))

        println(debugSerialize(data1, serializersModule = successSerializers).second)
        roundTrip(data1, successSerializers)
        sameSize(data1, data2, successSerializers)
    }
}

// These types are considered to be 3rd party types. -->
interface Poly
data class VariantA(val myInt: Int) : Poly
data class VariantB(val myLong: Long) : Poly
// <--

// This is a failing serializing strategy, because variant serializers use surrogates resulting in different lengths -->
object VariantAFailingSerializer : KSerializer<VariantA>
by (SurrogateSerializer(VariantAFailingSurrogate.serializer()) { VariantAFailingSurrogate(it.myInt) })

object VariantBFailingSerializer : KSerializer<VariantB>
by (SurrogateSerializer(VariantBFailingSurrogate.serializer()) { VariantBFailingSurrogate(it.myLong) })

@Suppress("ArrayInDataClass")
@Serializable
data class VariantAFailingSurrogate(val value: Int) : Surrogate<VariantA> {
    override fun toOriginal() = VariantA(value)
}

@Suppress("ArrayInDataClass")
@Serializable
data class VariantBFailingSurrogate(val value: Long) : Surrogate<VariantB> {
    override fun toOriginal() = VariantB(value)
}
// <-- Failing strategy end.

object VariantASucceedingSerializer : KSerializer<VariantA>
by (
    SurrogateSerializer(VariantASucceedingSurrogate.serializer()) {
        VariantASucceedingSurrogate(myInt = it.myInt)
    }
    )

object VariantBSucceedingSerializer : KSerializer<VariantB>
by (
    SurrogateSerializer(VariantBSucceedingSurrogate.serializer()) {
        VariantBSucceedingSurrogate(myLong = it.myLong)
    }
    )

@Serializable
abstract class PolyBaseSurrogate {
    abstract val myLong: Long?
    abstract val myInt: Int?
}

@Serializable
class VariantASucceedingSurrogate(
    override val myLong: Long? = null,
    override val myInt: Int?
) : PolyBaseSurrogate(), Surrogate<VariantA> {
    override fun toOriginal() = VariantA(myInt!!)
}

@Serializable
class VariantBSucceedingSurrogate(
    override val myLong: Long?,
    override val myInt: Int? = null
) : PolyBaseSurrogate(), Surrogate<VariantB> {
    override fun toOriginal() = VariantB(myLong!!)
}
