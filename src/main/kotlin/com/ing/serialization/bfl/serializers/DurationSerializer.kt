package com.ing.serialization.bfl.serializers

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.Serializable
import java.time.Duration

object DurationSerializer :
    SurrogateSerializer<Duration, DurationSurrogate>(DurationSurrogate.serializer(), { DurationSurrogate(it.seconds, it.nano) })

@Serializable
data class DurationSurrogate(
    val seconds: Long,
    val nanos: Int
) : Surrogate<Duration> {
    override fun toOriginal(): Duration = Duration.ofSeconds(seconds, nanos.toLong())
}
