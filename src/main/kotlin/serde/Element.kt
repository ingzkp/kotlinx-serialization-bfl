package serde

import annotations.DFLength
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.modules.SerializersModule

@ExperimentalSerializationApi
sealed class Element(val name: String) {
    abstract fun size(descriptor: SerialDescriptor? = null, serializersModule: SerializersModule? = null, defaults: List<Any>? = null): Int

    class Primitive(name: String, private val kind: SerialKind): Element(name) {
        override fun size(descriptor: SerialDescriptor?, serializersModule: SerializersModule?, defaults: List<Any>?): Int =
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

    class Strng(name: String, val requiredLength: Int): Element(name) {
        override fun size(descriptor: SerialDescriptor?, serializersModule: SerializersModule?, defaults: List<Any>?): Int =
            // SHORT (string length) + requiredLength * length(CHAR)
            2 + requiredLength * 2
    }

    // To be used to describe Collections (List/Map)
    class Collection(name: String, val sizingInfo: CollectionSizingInfo): CollectionElementSizingInfo by sizingInfo, Element(name) {
        override fun size(descriptor: SerialDescriptor?, serializersModule: SerializersModule?, defaults: List<Any>?): Int =
            // INT (collection length) + number_of_elements * sum_i { size(inner_i) }
            // = 4 + n * sum_i { size(inner_i) }
            4 + requiredLength * inner.sumBy { it.size(descriptor, serializersModule, defaults) }
    }

    class Structure(name: String, val inner: List<Element>, var isResolved: Boolean): Element(name) {
        override fun size(descriptor: SerialDescriptor?, serializersModule: SerializersModule?, defaults: List<Any>?): Int {
            // try if defaults can be picked up
            // dont forget polymorphics
            //


            // runCatching {
            //     // todo send the string representation of type there somehow
            //     //  serialName can be overridden, otherwise it coincides with the fully-qualified name
            //     Size.of(descriptor.serialName, serializersModule, defaults)
            // }
            //
            // else -> {
            // // TODO this branch is buggy
            // check(element is Element.Structure) { "Structure expected" }
            //
            // check(element.inner.size == descriptor.elementsCount)
            // { "Sizing info does not coincide with descriptors"}
            //
            // element.inner.zip(descriptor.elementDescriptors).sumBy { (childSizingInfo, childDescriptor) ->
            //     getElementSize(childDescriptor, childSizingInfo, serializersModule, defaults)
            // }
            return 0
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

    @ExperimentalSerializationApi
    companion object {
        const val polySerialNameLength = 100
        var dfQueue = ArrayDeque<Int>()

        fun parse(descriptor: SerialDescriptor, parentName: String? = null): Element {
            return when {
                descriptor.isStructure -> {
                    val inner = (0 until descriptor.elementsCount )
                        .map { idx ->
                            val lengths = descriptor.getElementAnnotations(idx)
                                .filterIsInstance<DFLength>()
                                .firstOrNull()?.lengths?.toList()?.let { ArrayDeque(it) }
                                ?: listOf()
                            dfQueue.prepend(lengths)

                            fromType(descriptor.serialName, descriptor.getElementDescriptor(idx))
                        }
                    Structure("${parentName ?:""}${descriptor.serialName}", inner, isResolved = false)
                }
                else -> TODO("Unimplemented")
            }
        }

        fun parseProperty(containerDescriptor: SerialDescriptor, propertyIdx: Int): Element {
            val descriptor = containerDescriptor.getElementDescriptor(propertyIdx)
            val name = "${containerDescriptor.serialName}.${descriptor.serialName}"

            val lengths = containerDescriptor.getElementAnnotations(propertyIdx)
                .filterIsInstance<DFLength>()
                .firstOrNull()?.lengths?.toList()?.let { ArrayDeque(it) }
                ?: listOf()

            dfQueue.prepend(lengths)

            return when {
                descriptor.isTrulyPrimitive -> Primitive(name, descriptor.kind)
                descriptor.isCollection || descriptor.isString || descriptor.isStructure ||
                    descriptor.isPolymorphic || descriptor.isContextual -> {
                    fromType(containerDescriptor.serialName, descriptor)
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

        fun fromType(parentName: String, descriptor: SerialDescriptor): Element {
            val name = "$parentName.${descriptor.serialName}"

            return when {
                descriptor.isTrulyPrimitive -> Primitive(name, descriptor.kind)
                descriptor.isString -> {
                    val requiredLength = dfQueue.removeFirstOrNull()
                        ?: throw SerdeError.InsufficientLengthData(parentName, descriptor)
                    Strng(name, requiredLength)
                }
                descriptor.isCollection -> {
                    val requiredLength = dfQueue.removeFirstOrNull()
                        ?: throw SerdeError.InsufficientLengthData(parentName, descriptor)
                    val children = descriptor.elementDescriptors.map {
                        fromType(name, it)
                    }
                    Collection(name, CollectionSizingInfo(requiredLength = requiredLength, inner = children))
                }
                descriptor.isStructure ->  {
                    // val children = descriptor.elementDescriptors.map {
                    //     fromType(name, it, lengths)
                    // }
                    val hasAnnotations = (descriptor.elementsCount - 1 downTo 0)
                        .any { idx -> descriptor.getElementAnnotations(idx).isNotEmpty() }

                    val children = if (hasAnnotations) {
                        listOf(parse(descriptor, parentName))
                    } else {
                        descriptor.elementDescriptors.map { fromType(name, it) }
                    }
                    Structure(name, inner = children, isResolved = true)
                }
                descriptor.isPolymorphic -> {
                    // Polymorphic type consists of a string and a structure.
                    dfQueue.prepend(polySerialNameLength)
                    val children = descriptor.elementDescriptors.map { fromType(name, it) }

                    Structure(name, inner = children, isResolved = true)
                }
                descriptor.isContextual -> Structure(name, inner = listOf(), isResolved = false)
                else -> error("Unreachable code when building element from type ${descriptor.serialName}")
            }
        }

        private fun <T> ArrayDeque<T>.prepend(value: T) {
            addFirst(value)
        }

        private fun <T> ArrayDeque<T>.prepend(list: List<T>) {
            addAll(0, list)
        }
    }
}

@ExperimentalSerializationApi
interface CollectionElementSizingInfo {
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
