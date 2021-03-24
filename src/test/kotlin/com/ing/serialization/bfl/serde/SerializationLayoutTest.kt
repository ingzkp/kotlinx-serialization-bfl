package com.ing.serialization.bfl.serde

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serializeX
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.EmptySerializersModule
import org.junit.jupiter.api.Test

@ExperimentalSerializationApi
@Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER")
class SerializationLayoutTest : SerdeTest() {
    @Serializable
    data class Data(@FixedLength([10]) val s: String = "123456789")

    @Test
    fun `layout is accessible`() {
        println(serializeX(Data(), EmptySerializersModule).second)
    }
}
