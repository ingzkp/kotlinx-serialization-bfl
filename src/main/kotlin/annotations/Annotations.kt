package annotations

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

@SerialInfo
@ExperimentalSerializationApi
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class KeyLength(val lengths: IntArray)

@SerialInfo
@ExperimentalSerializationApi
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class ValueLength(val lengths: IntArray)

@SerialInfo
@ExperimentalSerializationApi
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class DFLength(val lengths: IntArray)