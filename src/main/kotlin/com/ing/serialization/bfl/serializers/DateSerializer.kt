package com.ing.serialization.bfl.serializers

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import java.util.Date

object DateSerializer : KSerializer<Date> by (SurrogateSerializer(DateSurrogate.serializer()) { DateSurrogate(it.time) })

@Serializable
data class DateSurrogate(val l: Long) : Surrogate<Date> {
    override fun toOriginal(): Date = Date(l)

    companion object {
        fun from(date: Date): DateSurrogate = DateSurrogate(date.time)
    }
}
