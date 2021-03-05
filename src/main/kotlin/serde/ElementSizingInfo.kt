package serde

// TODO naming is not great
sealed class Element(val name: String) {
    class Primitive(name: String): Element(name)

    // To be used to describe Collections (List/Map) and Strings
    class Collection(name: String, val sizingInfo: CollectedSizingInfo): ElementSizingInfo by sizingInfo, Element(name)

    class Structure(name: String, val inner: List<Element> = listOf(), var resolved: Boolean = false): Element(name)

    fun copy(): Element  = when (this) {
        is Primitive -> this
        is Collection -> Collection(name, sizingInfo.copy())
        is Structure -> Structure(name, ArrayList(inner))
    }
}

interface ElementSizingInfo {
    var startByte: Int?
    var collectionActualSize: Int?
    var collectionRequiredSize: Int?
    var inner: List<Element>
}

data class CollectedSizingInfo(
    override var startByte: Int? = null,
    override var collectionActualSize: Int? = null,
    override var collectionRequiredSize: Int? = null,
    override var inner: List<Element> = mutableListOf(),
) : ElementSizingInfo

// // TODO naming is not great
// sealed class Element {
//     object Primitive: Element()
//
//     // To be used to describe Collections (List/Map) and Strings
//     class Compound(val sizingInfo: CollectedSizingInfo): ElementSizingInfo by sizingInfo, Element()
//
//     fun copy(): Element  = when (this) {
//         is Primitive -> this
//         is Compound -> Compound(sizingInfo.copy())
//     }
// }
//
// interface ElementSizingInfo {
//     //debug purposes
//     val name: String
//
//     var startByte: Int?
//     var collectionActualSize: Int?
//     var collectionRequiredSize: Int?
//     var inner: List<Element>
// }
//
// data class CollectedSizingInfo(
//     override val name: String,
//
//     override var startByte: Int? = null,
//     override var collectionActualSize: Int? = null,
//     override var collectionRequiredSize: Int? = null,
//     override var inner: List<Element> = mutableListOf(),
// ) : ElementSizingInfo