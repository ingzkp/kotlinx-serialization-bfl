package com.ing.serialization.bfl.serializers

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import sun.security.rsa.RSAPublicKeyImpl
import java.security.PublicKey

@ExperimentalSerializationApi
val serdeModule = SerializersModule {
    // Polymorphic types.
    polymorphic(PublicKey::class) {
        subclass(RSAPublicKeyImpl::class, RSAPublicKeySerializer)
    }
    //
    // Contextual types.
    contextual(DateSerializer)
}
