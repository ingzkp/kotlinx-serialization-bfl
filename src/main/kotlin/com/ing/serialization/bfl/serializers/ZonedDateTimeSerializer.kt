package com.ing.serialization.bfl.serializers

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.zone.ZoneRulesProvider

object ZonedDateTimeSerializer : KSerializer<ZonedDateTime> by (SurrogateSerializer(ZonedDateTimeSurrogate.serializer()) { ZonedDateTimeSurrogate.from(it) })

@Serializable
data class ZonedDateTimeSurrogate(
    val year: Int,
    val month: Byte,
    val dayOfMonth: Byte,
    val hour: Byte,
    val minute: Byte,
    val second: Byte,
    val nanoOfSecond: Int,
    val zoneIsOffset: Boolean,
    val zone: Int
) : Surrogate<ZonedDateTime> {

    override fun toOriginal(): ZonedDateTime {
        val zoneId = if (zoneIsOffset) {
            ZoneOffset.ofTotalSeconds(zone)
        } else {
            zoneRegionMap[zone]?.let { ZoneId.of(it) }
                ?: throw IllegalArgumentException("No ZoneId known by id: $zone")
        }
        return ZonedDateTime.of(year, month.toInt(), dayOfMonth.toInt(), hour.toInt(), minute.toInt(), second.toInt(), nanoOfSecond, zoneId)
    }

    companion object {
        private val zoneRegionMap: Map<Int, String> by lazy {
            ZoneRulesProvider.getAvailableZoneIds().associateBy {
                it.hashCode()
            }
        }

        fun from(original: ZonedDateTime) = with(original) {
            when (zone) {
                is ZoneOffset -> {
                    val offsetZone = zone as ZoneOffset
                    ZonedDateTimeSurrogate(year, month.value.toByte(), dayOfMonth.toByte(), hour.toByte(), minute.toByte(), second.toByte(), nano, true, offsetZone.totalSeconds)
                }
                else -> {
                    ZonedDateTimeSurrogate(year, month.value.toByte(), dayOfMonth.toByte(), hour.toByte(), minute.toByte(), second.toByte(), nano, false, zone.id.hashCode())
                }
            }
        }
    }
}
