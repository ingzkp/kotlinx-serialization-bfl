package annotations

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

@SerialInfo
@ExperimentalSerializationApi
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class FixedLength(val value: Int)
