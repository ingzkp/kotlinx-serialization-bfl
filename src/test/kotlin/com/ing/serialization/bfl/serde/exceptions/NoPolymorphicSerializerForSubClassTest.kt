package com.ing.serialization.bfl.serde.exceptions

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.serialization.bfl.api.serialize
import com.ing.serialization.bfl.serde.SerdeError
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

interface Base
data class Registered(val value: Int) : Base
data class UnRegistered(val value: Long) : Base

object RegisteredSerializer :
    SurrogateSerializer<Registered, RegisteredSurrogate>(RegisteredSurrogate.serializer(), { RegisteredSurrogate(it.value) })

@Serializable
@SerialName("SA")
data class RegisteredSurrogate(
    val value: Int
) : Surrogate<Registered> {
    override fun toOriginal() = Registered(value)
}

class NoPolymorphicSerializerForSubClassTest {
    private val baseSerializers = SerializersModule {
        polymorphic(Base::class) {
            subclass(Registered::class, RegisteredSerializer)
        }
    }

    @Serializable
    data class Data(val myValue: Base)

    @Test
    fun `unregistered subclass of polymorphic type should throw an error`() {
        val data = Data(UnRegistered(0))

        assertThrows<SerdeError.NoPolymorphicSerializerForSubClass> {
            serialize(data, serializersModule = baseSerializers)
        }
    }
}
