# Binary Fixed Length Serialization

![build status](https://github.com/ingzkp/kotlinx-serialization-bfl/actions/workflows/on-push.yml/badge.svg)

Binary Fixed Length (BFL) serialization format for [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization).

Binary fixed length protocol is dedicated to provide the binary representation of fixed
length to particular data type. 

Protocol supports primitive types, compounds and collections thereof, and allows for defining custom
serialization strategies.

##TOC
[Primitive types](#primitive-types)
[Compound types](#compound-types)
[Collections](#collections)
[Polymorphic types](#polymorphic-types)
[Configuring and Running](#configuring-serializers-and-running)

###Primitive types
Protocol supports the following primitive data types:
* bool
* byte
* short
* integer
* long
* char
   
###Compound types 
Types constituted of the primitive types. For example,
```kotlin
@Serializable
data class Compound(val int: Int, val byte: Byte)
```

###Collections   
Collections of the primitive types. To guarantee fixed length of a collection serialization,
max length of the collection must be specified. All collections, such as `String`, `List`, `Map`, must be annotated
with `FixedLength` annotation. This annotation specifies in a left-to-right (or, in other words, depth-first) fashion
length of the encountered collections. For example,
```kotlin
@Suppress("ArrayInDataClass")
@Serializable
data class ComplexType(
    @FixedLength([10]) // Number of elements in `ints` can be at most 10.
    val ints: IntArray,
    
    @FixedLength([5]) // String can contain at most 5 characters.
    val string: String,
    
    @FixedLength([4,     6,            8]) // Map - at most 4 elements, ByteArray - at most 6, String - at most 8 characters.
    val complex: Map<ByteArray, Pair<String, Int>>
)
```

###Polymorphic types
To ensure that each variant of a polymorphic type serializes to a fixed length byte array, each variant **MUST** use
the same representation or, called differently, surrogate. To aid the development, this module features several tools,
here we discuss how to use them.

Consider a third party class allowing for two variants:
```kotlin
interface Poly
data class VariantA(val myInt: Int) : Poly
data class VariantB(val myLong: Long) : Poly
```

A successful serialization strategy must define a surrogate accounting for both options.
```kotlin
abstract class PolyBaseSurrogate {
    abstract val myLong: Long?
    abstract val myInt: Int?
}

@Serializable
class VariantASucceedingSurrogate(
    override val myLong: Long? = null,
    override val myInt: Int?
) : PolyBaseSurrogate(), Surrogate<VariantA> {
    override fun toOriginal() = VariantA(myInt!!)
}

@Serializable
class VariantBSucceedingSurrogate(
    override val myLong: Long?,
    override val myInt: Int? = null
) : PolyBaseSurrogate(), Surrogate<VariantB> {
    override fun toOriginal() = VariantB(myLong!!)
}
```
Importantly, the type definitions within actual implementations (e.g., `VariantBSucceedingSurrogate`) **MUST NOT** override
those of the base surrogate (`PolyBaseSurrogate`), default values may be overridden. 

Finally, implementation of serializers is derived via a minor configuration.
```kotlin
object VariantASucceedingSerializer : KSerializer<VariantA> by (
    BaseSerializer(VariantASucceedingSurrogate.serializer()) {
        VariantASucceedingSurrogate(myInt = it.myInt)
    })

object VariantBSucceedingSerializer : KSerializer<VariantB> by (
    BaseSerializer(VariantBSucceedingSurrogate.serializer()) {
        VariantBSucceedingSurrogate(myLong = it.myLong)
    })
```

##Configuring serializers and running
After all serializers have been defined they must be grouped in the serializers module and passed further
to the BFL serializer. Consider the case of a polymorphic type [above](#polymorphic-types).  
```kotlin
val serializersModule = SerializersModule {
    polymorphic(Poly::class) {
        subclass(VariantA::class, VariantASucceedingSerializer)
        subclass(VariantB::class, VariantBSucceedingSerializer)
    }
    contextual(VariantASucceedingSerializer)
    contextual(VariantBSucceedingSerializer)
}
```
Both serializers for `Poly` are registered as polymorphic variants to allow type definitions such as `List<Poly>`
and as contextual types `List<VariantA>`.

The package has two version of serialization API -- using `reified` and accepting `Class<T>`.
They are invoked as follows
```kotlin
import com.ing.serialization.bfl.api.reified.serialize

val data = Data(/**/)
serialize(data, serializersModule)
```

```kotlin
import com.ing.serialization.bfl.api.serialize

val data = Data(/**/)
serialize(data, Data::class, serializersModule)
```


# How to release

A release can be made by creating a tag with the pattern `release/VERSION`. This will trigger a github action that will publish the jar file to the github maven repository, with version `VERSION`.

## How it works

In the github workflow `on-tag-publish.yml`, the version is parsed from the git tag, by removing the prefix `release/`. This version is set in the environment variable `GIT_RELEASE_VERSION`, and passed to gradle using the `-Pversion=$GIT_RELEASE_VERSION`.

For more information checkout the following files

- [on-tag-publish.yml](.github/workflows/on-tag-publish.yml)