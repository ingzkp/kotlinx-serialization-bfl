package serde

data class SerializingState(
    var currentByte: Int = 0,
    var lastStructureSize: Int = 0,
    val collectionSizingStack: ArrayDeque<ElementSizingInfo> = ArrayDeque())

data class ElementSizingInfo(
    var startByte: Int = 0,
    //must be positive or -1, the later indicates that the element is not list-like
    //-1 is used for fail-fast policy, such that users will know if they didn't annotate list-like structures
    var numberOfElements: Int = -1,
    var isRemovedRedundant: Boolean = false,
    var elementSize: Int = -1,
    var container: ElementSizingInfo? = null,
    val isPolymorphicKind: Boolean = false,
    //debug purposes
    val name: String
) {
    companion object {
        fun getRoot(name: String) = ElementSizingInfo(name = name)
    }
}