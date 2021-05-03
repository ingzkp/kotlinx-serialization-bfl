package com.ing.serialization.bfl.serde.exceptions

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import com.ing.serialization.bfl.api.serialize
import com.ing.serialization.bfl.serde.SerdeError
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import com.ing.serialization.bfl.api.reified.serialize as inlinedSerialize

class VariablePolymorphicSerialNameTest {
    @Serializable
    data class Transport(val plane: Plane)

    @Test
    fun `serialization of variants with serialName's  with different lengths should fail`() {
        val serializersModule = SerializersModule {
            polymorphic(Plane::class) {
                subclass(Airbus::class, AirbusSerializer)
                subclass(Boeing::class, BoeingSerializer)
            }
        }

        assertThrows<SerdeError.VariablePolymorphicSerialName> {
            serialize(Transport(Airbus()), serializersModule = serializersModule)
        }

        assertThrows<SerdeError.VariablePolymorphicSerialName> {
            inlinedSerialize(Transport(Airbus()), serializersModule = serializersModule)
        }
    }
}

// Treat them as 3rd party instances.
interface Plane
data class Airbus(val model: Int = 380) : Plane
data class Boeing(val model: Int = 787) : Plane
// <-- 3rd party.

object AirbusSerializer : KSerializer<Airbus>
by (SurrogateSerializer(AirbusSurrogate.serializer()) { AirbusSurrogate(it.model) })

object BoeingSerializer : KSerializer<Boeing>
by (SurrogateSerializer(BoeingSurrogate.serializer()) { BoeingSurrogate(it.model) })

@Serializable
@SerialName("0")
data class AirbusSurrogate(val model: Int) : Surrogate<Airbus> {
    override fun toOriginal() = Airbus(model)
}

@Serializable
@SerialName("00")
data class BoeingSurrogate(val model: Int) : Surrogate<Boeing> {
    override fun toOriginal() = Boeing(model)
}
