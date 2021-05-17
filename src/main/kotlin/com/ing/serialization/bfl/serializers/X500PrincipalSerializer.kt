package com.ing.serialization.bfl.serializers

import com.ing.serialization.bfl.annotations.FixedLength
import com.ing.serialization.bfl.api.Surrogate
import com.ing.serialization.bfl.api.SurrogateSerializer
import kotlinx.serialization.Serializable
import javax.security.auth.x500.X500Principal

@Serializable
data class X500PrincipalSurrogate(
    @FixedLength([PRINCIPAL_SIZE])
    val name: String
) : Surrogate<X500Principal> {
    override fun toOriginal(): X500Principal = X500Principal(name)

    companion object {
        /**
         * String representation of the X.500 distinguished name, according to [RFC2253](https://tools.ietf.org/html/rfc2253 "RFC2253").
         * The RFC does not define a size limit, so the size limit defined here is arbitrary, however we expect this to be
         * sufficient for real-world use-cases.
         */
        const val PRINCIPAL_SIZE = 1024
    }
}

object X500PrincipalSerializer :
    SurrogateSerializer<X500Principal, X500PrincipalSurrogate>(X500PrincipalSurrogate.serializer(), { X500PrincipalSurrogate(it.name) })
