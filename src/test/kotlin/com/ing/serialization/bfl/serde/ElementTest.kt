package com.ing.serialization.bfl.serde

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.serde.element.CollectionElement
import com.ing.serialization.bfl.serde.element.ElementFactory
import com.ing.serialization.bfl.serde.element.PrimitiveElement
import com.ing.serialization.bfl.serde.element.StringElement
import com.ing.serialization.bfl.serde.element.StructureElement
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.StructureKind
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ElementTest {
    @Serializable
    data class Inner(
        @FixedLength([3, 4])
        val value: Triple<String, Int, List<Int>>
    )

    @Serializable
    data class Outer(@FixedLength([2]) val value: List<Inner>)

    @Test
    fun `element with types is correct`() {
        val element = ElementFactory().parse(Inner.serializer().descriptor)

        assert(element is StructureElement)
        element as StructureElement
        element.inner.size shouldBe 1

        val inner1 = element.inner.first()
        assert(inner1 is StructureElement) { "`Data.value` must be described with StructureElement" }
        inner1 as StructureElement
        inner1.inner.size shouldBe 3

        val inner11 = inner1.inner[0]
        assert(inner11 is StringElement) { "`Triple.first` must be described with StringElement" }
        inner11 as StringElement
        inner11.requiredLength shouldBe 3

        val inner12 = inner1.inner[1]
        assert(inner12 is PrimitiveElement) { "`Triple.second` must be described with PrimitiveElement" }

        val inner13 = inner1.inner[2]
        assert(inner13 is CollectionElement) { "`Triple.third` must be described with CollectionElement" }
        inner13 as CollectionElement
        inner13.requiredLength shouldBe 4
    }

    @Test
    fun `element with classes is correct`() {
        val element = ElementFactory().parse(Outer.serializer().descriptor)

        // Top-level structure.
        assert(element is StructureElement)
        element as StructureElement
        element.inner.size shouldBe 1

        // List<..>
        val inner1 = element.inner.first()
        assert(inner1 is CollectionElement) { "`Data.value` must be described with CollectionElement" }
        inner1 as CollectionElement
        inner1.requiredLength shouldBe 2

        // Inner
        val inner2 = inner1.inner.first()
        assert(inner2 is StructureElement) { "`Inner` must be described with StructureElementElement" }
        inner2 as StructureElement
        inner2.inner.size shouldBe 1

        // Triple
        val inner3 = inner2.inner.first()
        assert(inner3 is StructureElement) { "`Inner.value` must be described with StructureElementElement" }
        inner3 as StructureElement
        inner3.inner.size shouldBe 3

        val inner31 = inner3.inner[0]
        assert(inner31 is StringElement) { "`Triple.first` must be described with Element.StringElement" }
        inner31 as StringElement
        inner31.requiredLength shouldBe 3

        val inner32 = inner3.inner[1]
        assert(inner32 is PrimitiveElement) { "`Triple.second` must be described with PrimitiveElement" }

        val inner33 = inner3.inner[2]
        assert(inner33 is CollectionElement) { "`Triple.third` must be described with CollectionElement" }
        inner33 as CollectionElement
        inner33.requiredLength shouldBe 4
    }

    @Test
    fun `layout generation`() {
        val element = ElementFactory().parse(Outer.serializer().descriptor)
        val layout = element.layout

        println(layout)
    }

    @Test
    fun `initialization of non-fixed primitive should fail`() {
        listOf(
            PrimitiveKind.STRING,
            StructureKind.LIST,
            PolymorphicKind.OPEN
        ).forEach {
            assertThrows<SerdeError.NotFixedPrimitive> {
                PrimitiveElement("", "", it, false)
            }
        }
    }

    @Test
    fun `padding of collection element with missing actual length should fail`() {
        assertThrows<IllegalArgumentException> {
            CollectionElement(
                "",
                "",
                listOf(),
                null,
                1,
                false
            ).padding
        }.also {
            it.message shouldContain "does not specify its actual length"
        }
    }
}
