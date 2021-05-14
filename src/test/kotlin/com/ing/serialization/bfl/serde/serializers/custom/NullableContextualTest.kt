package com.ing.serialization.bfl.serde.serializers.custom

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.serialization.bfl.api.debugSerialize
import io.kotest.matchers.shouldBe
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.jupiter.api.Test

class NullableContextualTest {
    @Serializable
    data class Data(val value: @Contextual Wrapper? = Wrapper())

    @Suppress("ArrayInDataClass")
    data class Wrapper(val bytes: ByteArray = byteArrayOf(1, 2, 3, 4, 5))

    @Serializable
    @Suppress("ArrayInDataClass")
    data class WrapperSurrogate(
        @FixedLength([10])
        val bytes: ByteArray
    ) : Surrogate<Wrapper> {
        override fun toOriginal(): Wrapper = Wrapper(bytes)
    }

    object WrapperSerializer :
        SurrogateSerializer<Wrapper, WrapperSurrogate>(WrapperSurrogate.serializer(), { WrapperSurrogate(it.bytes) })

    val serializersModule = SerializersModule {
        contextual(WrapperSerializer)
    }

    @Test
    fun `Serialization of nullable Contextual must produce correct layout`() {
        val data = Data()

        val serialization = debugSerialize(data, serializersModule = serializersModule)

        serialization.first.size shouldBe serialization.second.mask.sumBy { it.second }
    }
}
