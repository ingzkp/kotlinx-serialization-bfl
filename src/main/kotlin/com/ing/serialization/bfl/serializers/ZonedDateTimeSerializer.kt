package com.ing.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import java.time.ZoneId
import java.time.ZonedDateTime

object ZonedDateTimeSerializer : KSerializer<ZonedDateTime> by (SurrogateSerializer(ZonedDateTimeSurrogate.serializer()) { ZonedDateTimeSurrogate.from(it) })

@Serializable
data class ZonedDateTimeSurrogate(
    val year: Int,
    val month: Int,
    val dayOfMonth: Int,
    val hour: Int,
    val minute: Int,
    val second: Int,
    val nanoOfSecond: Int,
    @FixedLength([20])
    val zone: String
) : Surrogate<ZonedDateTime> {

    override fun toOriginal(): ZonedDateTime =
        ZonedDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond, ZoneId.of(zone))

    companion object {
        fun from(original: ZonedDateTime) = with(original) {
            ZonedDateTimeSurrogate(year, month.value, dayOfMonth, hour, minute, second, nano, zone.id)
        }
    }
}
