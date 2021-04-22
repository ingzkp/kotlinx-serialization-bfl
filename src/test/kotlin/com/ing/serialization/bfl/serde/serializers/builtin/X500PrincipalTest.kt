package com.ing.serialization.bfl.serde.serializers.builtin

import com.ing.serialization.bfl.api.debugSerialize
import com.ing.serialization.bfl.serde.checkedSerialize
import com.ing.serialization.bfl.serde.checkedSerializeInlined
import com.ing.serialization.bfl.serde.roundTrip
import com.ing.serialization.bfl.serde.roundTripInlined
import com.ing.serialization.bfl.serde.sameSize
import com.ing.serialization.bfl.serde.sameSizeInlined
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import javax.security.auth.x500.X500Principal

class X500PrincipalTest {
    @Serializable
    data class Data(
        val x500Principal: @Contextual X500Principal,
    )

    @Test
    fun `X500Principal should be serialized successfully`() {
        val mask = listOf(
            Pair("length", 2),
            Pair("name", 2048)
        )

        val data = Data(X500Principal("CN=Steve Kille,O=Isode Limited,C=GB"))
        println("${debugSerialize(data).second}")

        checkedSerializeInlined(data, mask)
        checkedSerialize(data, mask)
    }

    @Test
    fun `X500Principal should be the same after serialization and deserialization`() {
        listOf(
            Data(X500Principal("CN=Steve Kille,O=Isode Limited,C=GB")),
            Data(X500Principal("OU=Sales+CN=J. Smith,O=Widget Inc.,C=US")),
            Data(X500Principal("CN=L. Eagle,O=Sue\\, Grabbit and Runn,C=GB")),
            Data(X500Principal("CN=Before\\0DAfter,O=Test,C=GB")),
            Data(X500Principal("1.3.6.1.4.1.1466.0=#04024869,O=Test,C=GB")),
        ).forEach {
            roundTripInlined(it)
            roundTrip(it)
        }
    }

    @Test
    fun `different X500Principal should have same size after serialization`() {
        val data1 = Data(X500Principal("CN=Steve Kille,O=Isode Limited,C=GB"))
        val data2 = Data(X500Principal("OU=Sales+CN=J. Smith,O=Widget Inc.,C=US"))

        sameSize(data2, data1)
        sameSizeInlined(data2, data1)
    }
}
