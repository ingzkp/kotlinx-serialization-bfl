package element

import annotations.FixedLength
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import serde.Element
import serde.ElementFactory

@ExperimentalSerializationApi
class ElementTest {
    @Serializable
    data class Inner(
        @FixedLength([        3,            4])
        val value: Triple<String, Int, List<Int>>
    )

    @Serializable
    data class Outer(@FixedLength([2]) val value: List<Inner>)

    @Test
    fun `element with types is correct`() {
        val element = ElementFactory().parse(Inner.serializer().descriptor)

        assert(element is Element.Structure)
        element as Element.Structure
        element.inner.size shouldBe 1

        val inner1 = element.inner.first()
        assert(inner1 is Element.Structure) { "`Data.value` must be described with Element.Structure" }
        inner1 as Element.Structure
        inner1.inner.size shouldBe 3

        val inner11 = inner1.inner[0]
        assert(inner11 is Element.Strng) { "`Triple.first` must be described with Element.Strng" }
        inner11 as Element.Strng
        inner11.requiredLength shouldBe 3

        val inner12 = inner1.inner[1]
        assert(inner12 is Element.Primitive) { "`Triple.second` must be described with Element.Primitive" }

        val inner13 = inner1.inner[2]
        assert(inner13 is Element.Collection) { "`Triple.third` must be described with Element.Collection" }
        inner13 as Element.Collection
        inner13.requiredLength shouldBe 4
    }

    @Test
    fun `element with classes is correct`() {
        val element = ElementFactory().parse(Outer.serializer().descriptor)

        // Top-level structure.
        assert(element is Element.Structure)
        element as Element.Structure
        element.inner.size shouldBe 1

        // List<..>
        val inner1 = element.inner.first()
        assert(inner1 is Element.Collection) { "`Data.value` must be described with Element.Collection" }
        inner1 as Element.Collection
        inner1.requiredLength shouldBe 2

        // Inner
        val inner2 = inner1.inner.first()
        assert(inner2 is Element.Structure) { "`Inner` must be described with Element.Structure" }
        inner2 as Element.Structure
        inner2.inner.size shouldBe 1

        // Triple
        val inner3 = inner2.inner.first()
        assert(inner3 is Element.Structure) { "`Inner.value` must be described with Element.Structure" }
        inner3 as Element.Structure
        inner3.inner.size shouldBe 3

        val inner31 = inner3.inner[0]
        assert(inner31 is Element.Strng) { "`Triple.first` must be described with Element.Strng" }
        inner31 as Element.Strng
        inner31.requiredLength shouldBe 3

        val inner32 = inner3.inner[1]
        assert(inner32 is Element.Primitive) { "`Triple.second` must be described with Element.Primitive" }

        val inner33 = inner3.inner[2]
        assert(inner33 is Element.Collection) { "`Triple.third` must be described with Element.Collection" }
        inner33 as Element.Collection
        inner33.requiredLength shouldBe 4

    }

    @Test
    fun `layout generation`() {
        val element = ElementFactory().parse(Outer.serializer().descriptor)
        val layout = element.layout

        println(layout.toString(""))
    }
}
