package com.ing.serialization.bfl.serializers

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import java.time.Duration

object DurationSerializer : KSerializer<Duration> by (SurrogateSerializer(DurationSurrogate.serializer()) { DurationSurrogate(it.seconds, it.nano) })

@Serializable
data class DurationSurrogate(
    val seconds: Long,
    val nanos: Int
) : Surrogate<Duration> {
    override fun toOriginal(): Duration = Duration.ofSeconds(seconds, nanos.toLong())

    companion object {
        fun from(duration: Duration): DurationSurrogate = DurationSurrogate(duration.seconds, duration.nano)
    }
}
