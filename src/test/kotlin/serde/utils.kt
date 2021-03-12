package serde

import annotations.DFLength
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
data class Own(val int: Int = 100) {
    override fun toString() = "Own(int= $int)"
}

@ExperimentalSerializationApi
@Serializable
data class OwnList(@DFLength([2]) val list: List<Int> = listOf(1)) {
    override fun toString() = "OwnList(list= ${list.joinToString()})"
}