package com.ing.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import javax.security.auth.x500.X500Principal

@Serializable
data class X500PrincipalSurrogate(
    /**
     * String representation of the X.500 distinguished name, according to [RFC2253](https://tools.ietf.org/html/rfc2253 "RFC2253").
     * The RFC does not define a size limit, so the size limit defined here is arbitrary, however we expect this to be
     * sufficient for real-world use-cases.
     */
    @FixedLength([1024])
    val name: String
) : Surrogate<X500Principal> {
    override fun toOriginal(): X500Principal = X500Principal(name)
}

object X500PrincipalSerializer : KSerializer<X500Principal>
by (SurrogateSerializer(X500PrincipalSurrogate.serializer()) { X500PrincipalSurrogate(it.name) })
