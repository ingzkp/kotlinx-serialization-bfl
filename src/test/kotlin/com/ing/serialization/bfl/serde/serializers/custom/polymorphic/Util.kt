package com.ing.serialization.bfl.serde.serializers.custom.polymorphic

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import sun.security.provider.DSAPublicKeyImpl
import sun.security.rsa.RSAPublicKeyImpl
import java.security.PublicKey

val PolySerializers = SerializersModule {
    // Polymorphic types.
    polymorphic(PublicKey::class) {
        subclass(RSAPublicKeyImpl::class, RSAPublicKeySerializer)
        subclass(DSAPublicKeyImpl::class, DSAPublicKeySerializer)
    }
    contextual(RSAPublicKeySerializer)
    contextual(DSAPublicKeySerializer)
}
