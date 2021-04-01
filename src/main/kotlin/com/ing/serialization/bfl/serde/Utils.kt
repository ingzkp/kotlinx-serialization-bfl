package com.ing.serialization.bfl.serde

fun <T> ArrayDeque<T>.prepend(value: T) {
    addFirst(value)
}

fun <T> ArrayDeque<T>.prepend(list: List<T>) {
    addAll(0, list)
}
