package com.ing.serialization.bfl.serializers

import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

object UUIDSerializer : KSerializer<UUID> by (SurrogateSerializer(UUIDSurrogate.serializer()) { UUIDSurrogate(it.mostSignificantBits, it.leastSignificantBits) })

@Serializable
data class UUIDSurrogate(
    val mostSigBits: Long,
    val leastSigBits: Long
) : Surrogate<UUID> {
    override fun toOriginal(): UUID = UUID(mostSigBits, leastSigBits)
}
