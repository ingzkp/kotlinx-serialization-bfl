package serde

// TODO naming is not great
sealed class Element(val name: String) {
    class Primitive(name: String): Element(name)

    // To be used to describe Collections (List/Map) and Strings
    class Collection(name: String, val sizingInfo: CollectedSizingInfo): ElementSizingInfo by sizingInfo, Element(name)

    class Structure(name: String, val inner: List<Element> = listOf(), var isResolved: Boolean): Element(name)

    fun copy(): Element  = when (this) {
        is Primitive -> this
        is Collection -> Collection(name, sizingInfo.copy())
        is Structure -> Structure(name, ArrayList(inner), isResolved)
    }
}

interface ElementSizingInfo {
    var startByte: Int?
    var collectionActualLength: Int?
    var collectionRequiredLength: Length?
    var inner: List<Element>
}

data class CollectedSizingInfo(
    override var startByte: Int? = null,
    override var collectionActualLength: Int? = null,
    override var collectionRequiredLength: Length? = null,
    override var inner: List<Element> = mutableListOf(),
) : ElementSizingInfo

sealed class Length {
    object Actual: Length()
    data class Fixed(val value: Int): Length()
}