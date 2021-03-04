fun main() {
    // val serializersModule = SerializersModule {
    //     polymorphic(PublicKey::class) {
    //         subclass(RSAPublicKeyImpl::class, RSAPublicKeySerializer)
    //     }
    // }

    /*
    val data = CoverAll(
        "12345678901234567890",
        listOf(),
        listOf(listOf(listOf(1), listOf(2), listOf(3)), listOf(listOf(4), listOf(5), listOf(6))),
        listOf(Pair(1, 2)),
        c,
        listOf(Own(25)),
        getRSA(),
        mapOf("a" to listOf(1), "b" to listOf(2))
    )
    val splitMask = listOf(
        Pair("string.length", 2),
        Pair("string.value", 2 * 25),
        Pair("dates.length", 4),
        Pair("dates.value", 8 * 3),
        Pair("listMatrix.length", 4),
        Pair("listMatrix[0].length", 4),
        Pair("listMatrix[0][0].length", 4),
        Pair("listMatrix[0][0].value", 4 * 5),
        Pair("listMatrix[0][1].length", 4),
        Pair("listMatrix[0][1].value", 4 * 5),
        Pair("listMatrix[0][2].length", 4),
        Pair("listMatrix[0][2].value", 4 * 5),
        Pair("listMatrix[0][3].length", 4),
        Pair("listMatrix[0][3].value", 4 * 5),
        Pair("listMatrix[1].length", 4),
        Pair("listMatrix[1][0].length", 4),
        Pair("listMatrix[1][0].value", 4 * 5),
        Pair("listMatrix[1][1].length", 4),
        Pair("listMatrix[1][1].value", 4 * 5),
        Pair("listMatrix[1][2].length", 4),
        Pair("listMatrix[1][2].value", 4 * 5),
        Pair("listMatrix[1][3].length", 4),
        Pair("listMatrix[1][3].value", 4 * 5),
        Pair("listMatrix[2].length", 4),
        Pair("listMatrix[2][0].length", 4),
        Pair("listMatrix[2][0].value", 4 * 5),
        Pair("listMatrix[2][1].length", 4),
        Pair("listMatrix[2][1].value", 4 * 5),
        Pair("listMatrix[2][2].length", 4),
        Pair("listMatrix[2][2].value", 4 * 5),
        Pair("listMatrix[2][3].length", 4),
        Pair("listMatrix[2][3].value", 4 * 5),
        Pair("pairs.length", 4),
        Pair("pairs.value", 2 * (4 + 4)),
        Pair("date", 8),
        Pair("owns.length", 4),
        Pair("owns.value", 2 * 4),
        Pair("publicKey.length", 4),
        Pair("publicKey.length", 500)
    )
    println(data)

    val output = ByteArrayOutputStream()
    val serializersModule = SerializersModule {
        polymorphic(PublicKey::class) {
            subclass(RSAPublicKeyImpl::class, RSAPublicKeySerializer)
        }
    }
    encodeTo(DataOutputStream(output), data, serializersModule, Own(), DateSurrogate(Long.MIN_VALUE))
    val bytes = output.toByteArray()

    println("Serialized:")
    var start = 0
    splitMask.forEach {
        val range = bytes.copyOfRange(start, start + it.second)
        val repr = range.joinToString(separator = ",") { d -> String.format("%2d", d) }
        // val repr = range.toAsciiHexString()
        println("${it.first} [$start, ${start + it.second}]\t: $repr")
        start += it.second
    }

    val deserialized = decodeFrom<CoverAll>(DataInputStream(ByteArrayInputStream(bytes)))
    println(deserialized)
*/
    // val data = MapMe()
    // val output = ByteArrayOutputStream()
    // encodeTo(DataOutputStream(output), data, serializersModule)
    // val bytes = output.toByteArray()
    // println("Serialized: ${bytes.joinToString(separator = ",")}")
    // val deserialized = decodeFrom<Outer>(DataInputStream(ByteArrayInputStream(bytes)))
    // println(deserialized)
}
//
// @Serializable
// @ExperimentalSerializationApi
// data class CoverAll(
//     @ValueLength([25])
//     val string: String,
//
//     @ValueLength([3])
//     val dates: List<@Serializable(with = DateSerializer::class) Date>,
//
//     @ValueLength([3, 4, 5])
//     val listMatrix: List<List<List<Int>>>,
//
//     @ValueLength([2])
//     val pairs: List<Pair<Int, Int>>,
//
//     @Serializable(with = DateSerializer::class)
//     val date: Date,
//
//     @ValueLength([2])
//     val owns: List<Own>,
//
//     @Serializable(with = RSAPublicKeySerializer::class)
//     val publicKey: PublicKey,
//
//     @KeyLength([2])
//     @ValueLength([4])
//     val map: Map<String, List<Int>>
//
//     // Empty List of Strings. I expect it to fail because it does not set lastStructureSize.
// )
//
// @Serializable
// @ExperimentalSerializationApi
// data class MapMe(
//     // @DFLength([2, 4,     2,     3,   5,   3])
//     // val map: Map<List<String>, List<Map<String, Int>>> =
//     //     mapOf(listOf("a") to listOf(), listOf("b") to listOf(mapOf("c" to 1)))
//
//     @DFLength([2, 4,     2])
//     val map: Map<String, List<Int>> =
//         mapOf("a" to listOf(), "b" to listOf(1))
// )
//
//
// fun getRSA(): PublicKey {
//     val generator = KeyPairGenerator.getInstance("RSA")
//     generator.initialize(2048, SecureRandom())
//     return generator.genKeyPair().public
// }