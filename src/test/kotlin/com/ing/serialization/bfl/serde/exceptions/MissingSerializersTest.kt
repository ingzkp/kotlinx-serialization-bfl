package com.ing.serialization.bfl.serde.exceptions

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.debugSerialize
import com.ing.serialization.bfl.api.deserialize
import com.ing.serialization.bfl.api.serialize
import com.ing.serialization.bfl.serde.SerdeError
import io.kotest.matchers.string.shouldContain
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

    @Test
    fun `serialization and deserialization with missing top level serializer should fail`() {
        data class Data(val value: Int)

        assertThrows<SerdeError.NoTopLevelSerializer> {
            serialize(Data(1))
        }

        assertThrows<SerdeError.NoTopLevelSerializer> {
            debugSerialize(Data(1))
        }

        assertThrows<SerdeError.NoTopLevelSerializer> {
            deserialize(byteArrayOf(), Data::class.java)
        }
    }

    @Serializable
    open class Dummy

    @Serializable
    object DummyObject : Dummy()

    @Test
    fun `serialization with unknown serializer should fail`() {
        assertThrows<IllegalStateException> {
            serialize(DummyObject)
        }.also {
            it.message shouldContain "not supported"
        }
    }

    @Test
    fun `serialization with unknown serializer should fail v2`() {
        @Serializable
        data class Data(@FixedLength([2]) val list: List<DummyObject>)

        assertThrows<IllegalStateException> {
            serialize(Data(listOf(DummyObject)))
        }.also {
            it.message shouldContain "Do not know how to build element from type"
        }
    }
}
