package com.ing.serialization.bfl.serde.exceptions

import com.ing.serialization.bfl.api.serialize
import com.ing.serialization.bfl.serde.SerdeError
import kotlinx.serialization.Contextual
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MissingSerializersTest {
    @Test
    fun `serialization with missing Contextual serializer should fail`() {
        @Serializable
        data class Data(val value: @Contextual Int)

        assertThrows<SerdeError.NoContextualSerializer> {
            serialize(Data(1))
        }
    }

    @Test
    fun `serialization with missing Polymorphic serializer should fail`() {
        @Serializable
        data class Data(val value: @Polymorphic Int)

        assertThrows<SerdeError.NoPolymorphicSerializers> {
            serialize(Data(1))
        }
    }
}
