package element

import annotations.DFLength
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import serde.Element

@ExperimentalSerializationApi
class ElementTest {
    @Test
    fun `element with types is correct`() {
        @Serializable
        data class Data(
            @DFLength([        2,            3])
            val value: Triple<@Tess String, Int, List<Int>>
            )

        val element = Element.parse(Data.serializer().descriptor)
        println("hey hey hey you guys are okay")
    }

    @Test
    fun `element with classes is correct`() {
        @Serializable
        data class Inner(@DFLength([3, 4]) val value: Triple<String, Int, List<Int>>)

        @Serializable
        data class Data(@DFLength([2]) val value: List<Inner>)

        val element = Element.parse(Data.serializer().descriptor)
        println("hey hey hey you guys are okay")
    }
}

@SerialInfo
@Target(AnnotationTarget.TYPE)
annotation class Tess