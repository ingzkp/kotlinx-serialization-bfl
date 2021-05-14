package com.ing.serialization.bfl.serializers

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.Serializable
import java.util.Date

object DateSerializer : SurrogateSerializer<Date, DateSurrogate>(DateSurrogate.serializer(), { DateSurrogate(it.time) })

@Serializable
data class DateSurrogate(val l: Long) : Surrogate<Date> {
    override fun toOriginal(): Date = Date(l)
}
