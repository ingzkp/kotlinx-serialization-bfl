package com.ing.serialization.bfl.serde.element

class Layout(
    val name: String,
    val mask: List<Pair<String, Int>>,
    val inner: List<Layout>
) {
    private fun toString(prefix: String = ""): String {
        val deepPrefix = "$prefix "
        return "$prefix$name\n$deepPrefix" +
            mask.joinToString(separator = "\n$deepPrefix") { "${it.first} - ${it.second}" } +
            "\n" +
            if (inner.isNotEmpty()) {
                inner.joinToString(separator = "") { it.toString(deepPrefix) }
            } else {
                ""
            }
    }

    override fun toString() = toString("")
}
