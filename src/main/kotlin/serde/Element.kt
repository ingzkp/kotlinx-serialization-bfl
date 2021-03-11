package serde

import annotations.DFLength
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import prepend

@ExperimentalSerializationApi
sealed class Element(val name: String) {
    abstract val size: Int

    class Primitive(name: String, private val kind: SerialKind): Element(name) {
        override val size by lazy {
            when (kind) {
                is PrimitiveKind.BOOLEAN -> 1
                is PrimitiveKind.BYTE -> 1
                is PrimitiveKind.SHORT -> 2
                is PrimitiveKind.INT -> 4
                is PrimitiveKind.LONG -> 8
                is PrimitiveKind.FLOAT -> throw IllegalStateException("Floats are not yet supported")
                is PrimitiveKind.DOUBLE -> throw IllegalStateException("Double are not yet supported")
                is PrimitiveKind.CHAR -> 2
                else -> throw IllegalStateException("$name is called primitive while it is not")
            }
        }
    }

    class Strng(name: String, val requiredLength: Int): Element(name) {
        override val size by lazy {
            // SHORT (string length) + requiredLength * length(CHAR)
            2 + requiredLength * 2
        }
    }

    // To be used to describe Collections (List/Map)
    class Collection(name: String, val sizingInfo: CollectionSizingInfo): CollectionElementSizingInfo by sizingInfo, Element(name) {
        override val size by lazy {
            // INT (collection length) + number_of_elements * sum_i { size(inner_i) }
            // = 4 + n * sum_i { size(inner_i) }
            4 + requiredLength * elementSize
        }

        val elementSize by lazy {
            inner.sumBy { it.size }
        }
    }

   class Structure(name: String, val inner: List<Element>, var isResolved: Boolean) : Element(name) {
       override val size by lazy {
           inner.sumBy { it.size }
       }
   }

    // TODO why copy?
    fun copy(): Element  = when (this) {
        is Primitive -> this
        is Strng -> this
        is Collection -> Collection(name, sizingInfo.copy())
        is Structure -> Structure(name, ArrayList(inner), isResolved)
    }

    inline fun <reified T: Element> expect(): T {
        // Non-null assertion is fine because T is bound to Element.
        (this as? T) ?: throw SerdeError.WrongElement(T::class.simpleName!!, this)
        return this
    }

    companion object {
        private var dfQueue = ArrayDeque<Int>()
        fun parseProperty(containerDescriptor: SerialDescriptor, propertyIdx: Int): Element {
            val descriptor = containerDescriptor.getElementDescriptor(propertyIdx)
            val name = "${containerDescriptor.serialName}.${descriptor.serialName}"

            val lengths = containerDescriptor.getElementAnnotations(propertyIdx)
                .filterIsInstance<DFLength>()
                .firstOrNull()?.lengths?.toList()?.let { ArrayDeque(it) }
                ?: listOf()

            dfQueue.prepend(lengths)

            return when {
                descriptor.isTrulyPrimitive -> Element.Primitive(name, descriptor.kind)
                descriptor.isCollection || descriptor.isString || descriptor.isStructure ||
                    descriptor.isPolymorphic || descriptor.isContextual -> {
                    fromType(containerDescriptor.serialName, descriptor, EmptySerializersModule)
                }
                else -> error("Error processing property ${descriptor.serialName}")
            }

            // return when {
            //     descriptor.isTrulyPrimitive -> Primitive(name, descriptor.kind)
            //     descriptor.isCollection || descriptor.isString -> {
            //         fromType(containerDescriptor.serialName, descriptor)
            //     }
            //     descriptor.isStructure -> {
            //         // Classes may have inner collections buried deep inside.
            //         // We can infer this by observing an appropriate length annotation.
            //         val lengths = containerDescriptor.getElementAnnotations(propertyIdx)
            //             .filterIsInstance<DFLength>()
            //             .firstOrNull()?.lengths?.toList()?.let { ArrayDeque(it) }
            //         if (lengths != null) {
            //             dfQueue.prepend(lengths)
            //             fromType(containerDescriptor.serialName, descriptor)
            //         } else {
            //             Structure(name, inner = listOf(), isResolved = false)
            //         }
            //     }
            //     descriptor.isPolymorphic || descriptor.isContextual -> {
            //         fromType(containerDescriptor.serialName, descriptor)
            //     }
            //     else -> error("Error processing property ${descriptor.serialName}")
            // }
        }

        fun fromType(parentName: String, descriptor: SerialDescriptor, serializersModule: SerializersModule): Element {
            TODO()
        }
    }
}

@ExperimentalSerializationApi
interface CollectionElementSizingInfo {
    // TODO remove after the decoder is finished
    var startByte: Int?
    var actualLength: Int?
    var requiredLength: Int
    var inner: List<Element>
}

@ExperimentalSerializationApi
data class CollectionSizingInfo(
    override var startByte: Int? = null,
    override var actualLength: Int? = null,
    override var requiredLength: Int,
    override var inner: List<Element> = mutableListOf(),
) : CollectionElementSizingInfo
