package com.ing.serialization.bfl.serializers

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

object InstantSerializer :
    SurrogateSerializer<Instant, InstantSurrogate>(InstantSurrogate.serializer(), { InstantSurrogate.from(it) })

@Serializable
data class InstantSurrogate(
    val seconds: Long,
    val nanos: Int
) : Surrogate<Instant> {
    override fun toOriginal(): Instant = Instant.ofEpochSecond(seconds, nanos.toLong())

    companion object {
        fun from(original: Instant): InstantSurrogate = InstantSurrogate(original.epochSecond, original.nano)
    }
}
