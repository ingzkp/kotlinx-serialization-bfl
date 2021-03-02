package serde

data class ElementSerializingMeta(
    var startByte: Int? = null,
    //must be positive or -1, the later indicates that the element is not list-like
    //-1 is used for fail-fast policy, such that users will know if they didn't annotate list-like structures
    var numberOfElements: Int? = null,
    var inner: ElementSerializingMeta? = null,
    //debug purposes
    val name: String
)