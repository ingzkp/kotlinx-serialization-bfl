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

interface Parent
data class Child(val value: Int) : Parent

object ChildSerializer : KSerializer<Child>
by (SurrogateSerializer(ChildSurrogate.serializer()) { ChildSurrogate(it.value) })

@Serializable
@SerialName("CHL")
data class ChildSurrogate(
    val value: Int
) : Surrogate<Child> {
    override fun toOriginal() = Child(value)
}

class NoSurrogateSerializerTest {
    private val parentSerializers = SerializersModule {
        polymorphic(Parent::class) {
            subclass(Child::class, ChildSerializer)
        }
    }

    @Serializable
    data class Data(val myValue: Parent)

    @Test
    fun `serializer of polymorphic type not inheriting from SurrogateSerializer should throw an error`() {
        val data = Data(Child(0))

        assertThrows<SerdeError.NoSurrogateSerializer> {
            serialize(data, serializersModule = parentSerializers)
        }
    }
}
