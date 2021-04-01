package com.ing.serialization.bfl.serde

import com.ing.serialization.bfl.annotations.FixedLength
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import com.ing.serialization.bfl.api.reified.debugSerialize as debugSerializeInlined

@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
class SerializationLayoutTest {
    @Serializable
    data class Data(@FixedLength([10]) val s: String = "123456789")

    @Test
    fun `layout is accessible`() {
        println(debugSerializeInlined(Data()).second)
    }
}
