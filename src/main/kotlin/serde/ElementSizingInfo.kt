package serde

// TODO naming is not great
sealed class SizingInfo {
    object Bounded: SizingInfo()

    // To be used to describe Collections, Strings and Structures
    class Compound(val sizingInfo: ElementSizingInfoImpl): ElementSizingInfo by sizingInfo, SizingInfo()

    fun copy(): SizingInfo  = when (this) {
        is Bounded -> this
        is Compound -> Compound(sizingInfo.copy())
    }
}

interface ElementSizingInfo {
    //debug purposes
    val name: String

    var startByte: Int?
    var collectionActualSize: Int?
    var collectionRequiredSize: Int?
    var inner: List<SizingInfo>
}

data class ElementSizingInfoImpl(
    override val name: String,

    override var startByte: Int? = null,
    override var collectionActualSize: Int? = null,
    override var collectionRequiredSize: Int? = null,
    override var inner: List<SizingInfo> = mutableListOf(),
) : ElementSizingInfo