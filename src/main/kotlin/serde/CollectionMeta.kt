package serde

data class CollectionMeta(
    var start: Int?,
    var occupies: Int?,
    val annotations: List<Annotation>,
    val free: MutableMap<String, Any>
)