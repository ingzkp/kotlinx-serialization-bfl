package com.ing.serialization.bfl.serde

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class SerdeErrorTest {
    @Test
    fun `full class name is included in NoTopLevelSerializer message`() {
        SerdeError.NoTopLevelSerializer(String::class.java).message shouldBe
            "Top-level serializer absent for java.lang.String"
    }

    @Test
    fun `full kclass name is included in NoTopLevelSerializer message`() {
        SerdeError.NoTopLevelSerializer(String::class, IllegalStateException()).message shouldBe
            "Top-level serializer absent for kotlin.String"
    }
}
