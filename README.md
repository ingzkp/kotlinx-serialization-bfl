# Binary Fixed Length Serialization

![build status](https://github.com/ingzkp/kotlinx-serialization-bfl/actions/workflows/on-push.yml/badge.svg)

Binary Fixed Length (BFL) serialization format for [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization).

Binary fixed length protocol is dedicated to provide the binary representation of fixed
length to particular data type. 

Protocol supports primitive types, compounds and collections thereof, and allows for defining custom
serialization strategies.

## TOC
- [Primitive types](#primitive-types)
- [Compound types](#compound-types)
- [Collections](#collections)
- [Implementing Serializers for existing types](#implementing-serializers-for-existing-types)
- [Classes with Generics](#classes-with-generics)
- [Polymorphic types](#polymorphic-types)
- [Configuring and Running](#configuring-serializers-and-running)

### Primitive types
Protocol supports the following primitive data types:
* bool
* byte
* short
* integer
* long
* char
   
### Compound types
Types constituted of the primitive types. For example,
```kotlin
@Serializable
data class Compound(val int: Int, val byte: Byte)
```

### Collections
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

### Implementing Serializers for existing types
In cases where a pre-defined type needs to be serialized, meaning that you cannot add `@Serializable` or `@FixedLength`
annotations, one can introduce a surrogate class, and transform to and from that type. This library offers the
`Surrogate` and `SurrogateSerializer` classes to aid with this implementation.

Consider the following type:
```kotlin
data class CustomData(val value: String)
```

There are two things that this class is missing:
1. A `@Serializable` annotation
2. A `@FixedLength` annotation on the `String` field

First we define the surrogate class, note that this implements `Surrogate<CustomData>`. The `toOriginal()` method is
used to convert the surrogate back to the original type after deserialization.
```kotlin
@Serializable
data class CustomDataSurrogate(
        @FixedLength([42])
        val value: String
) : Surrogate<CustomData> {
    override fun toOriginal(): CustomData = CustomData(value)
}
```

Next we define the serializer, using `SurrogateSerializer`. The constructor of `SurrogateSerializer` takes a lambda that is used
to convert the actual value into the surrogate type before serialization.
```kotlin
object CustomDataSerializer : KSerializer<CustomData>
by (SurrogateSerializer(CustomDataSurrogate.serializer()) {
    CustomDataSurrogate(it.value)
})
```

Then we add the serializer to a `SerializersModule`.
```kotlin
import kotlinx.serialization.modules.contextual

val customDataSerializationModule = SerializersModule {
    contextual(CustomDataSerializer)
}
```

Finally, we can serialize and deserialize `CustomData` instances.
```kotlin
import com.ing.serialization.bfl.api.reified.deserialize
import com.ing.serialization.bfl.api.reified.serialize

val original = CustomData("Hello World!")
val serializedBytes = serialize(original, customDataSerializationModule)
val deserialized: CustomData = deserialize(serializedBytes, customDataSerializationModule)
assert(deserialized == original) { "Expected $deserialized to be $original" }
```

### Classes with Generics
For classes with generics there are two distinct cases, custom [classes that can be annotated with `@Serializable`](#project-classes), and
[third-party classes that cannot be annotated with `@Serializable`](#third-party-classes).
Kotlinx-serialization allows for configuring contextual serializers, e.g. 
```kotlin
val serializers = SerializersModule {
    contextual(/* someSerializer */)
}
```
Unfortunately in case of generics all serializers corresponding to different generic parameters will be attempted to be
registered for the base generic class ([see](https://github.com/Kotlin/kotlinx.serialization/pull/1406)). For this
reason, both reified and non-reified version for serialization and deserialization allow for explicit specification of
the serialization strategy. Alternatively you can register a single contextual serializer for a specific variant of
a generic class.

#### Project Classes 
Consider a project class with generics, like the following:
```kotlin
@Serializable
data class CustomData<T>(
    val value: T
)
```

Instances of this class can be serialized and deserialized normally for standard supported types, like `Int`, and types
for which the fixed serialized length can be automatically derived. For the project classes it is recommended to use
the `reified` versions of the serialization API.
```kotlin
import com.ing.serialization.bfl.api.reified.deserialize
import com.ing.serialization.bfl.api.reified.serialize

val original = CustomData(42)
val serializedBytes = serialize(original)
val deserialized: CustomData<Int> = deserialize(serializedBytes)
assert(deserialized == original) { "Expected $deserialized to be $original" }
```

When the serialization length cannot be derived for the embedded type, like in `CustomData<Currency>`, one must resort
to the surrogate approach, as described in
[Implementing Serializers for existing types](#implementing-serializers-for-existing-types).
Conveniently, for some Java types an appropriate serializer is implemented and registered in `BFLSerializers`. Below
both approaches with a specific strategy and with a SerializersModule are presented.

```kotlin
import com.ing.serialization.bfl.api.reified.deserialize
import com.ing.serialization.bfl.api.reified.serialize
import com.ing.serialization.bfl.serializers.CurrencySerializer
import com.ing.serialization.bfl.serializers.BFLSerializers

val original = CustomData(Currency.getInstance(Locale.ITALY))
val strategy = CustomData.serializer(CurrencySerializer)
val serializedBytes = serialize(original, strategy = null, serializersModule = BFLSerializers)
val deserialized: CustomData<Currency> = deserialize(serializedBytes, strategy = strategy, serializersModule = EmptySerializersModule)
assert(deserialized == original) { "Expected $deserialized to be $original" }
```

For more information see [ProjectGenericsTest][2].

#### Third-party Classes
The surrogate approach can be followed when implementing serializers for third-party classes with generics.
Consider the following class, which is out of our control and cannot be annotated with `@Serializable`: 

```kotlin
data class CustomData<T>(
    val value: T
)
```

For instances of `CustomData<String>`, use the following surrogate and serializer:
```kotlin
@Serializable
data class CustomDataStringSurrogate(
    @FixedLength([42])
    val value: String
) : Surrogate<CustomData<String>> {
    override fun toOriginal(): CustomData<String> = CustomData(value)
}

object CustomDataStringSerializer : KSerializer<CustomData<String>>
by (SurrogateSerializer(CustomDataStringSurrogate.serializer()) {
        CustomDataStringSurrogate(it.value)
})
```
This strategy must be explicitly provided when serializing or deserializing the respective type.

For instances of `CustomData` using types that have no serializer, use `@Contextual`, as in the following example:
```kotlin
@Serializable
data class CustomDataCurrencySurrogate(
    val value: @Contextual Currency
) : Surrogate<CustomData<Currency>> {
    override fun toOriginal(): CustomData<Currency> = CustomData(value)
}

object CustomDataCurrencySerializer : KSerializer<CustomData<Currency>>
by (SurrogateSerializer(CustomDataCurrencySurrogate.serializer()) {
        CustomDataCurrencySurrogate(it.value)
})
```

When serializing or deserializing object of classes with generics, pass the serializers explicitly.
```kotlin
import com.ing.serialization.bfl.api.reified.deserialize
import com.ing.serialization.bfl.api.reified.serialize

val original = CustomData(Currency.getInstance(Locale.JAPAN))
val serializedBytes = serialize(original, CustomDataCurrencySerializer)
val deserialized: CustomData<Currency> = deserialize(serializedBytes, CustomDataCurrencySerializer)
assert(deserialized == original) { "Expected $deserialized to be $original" }
```

For more information see [ThirdPartyGenericsTest][3].

### Polymorphic types
Fixed length serialization of polymorphic types is non-trivial. Concrete implementations of a polymorphic type
can have different sets of properties and thus in general, by construction, will not be serialized into the same length
sequence of bytes. One way to address this limitation is to use the same surrogate for all required variants of the polymorphic type.
To aid the development, this module features several tools, which we discuss below.

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
Importantly, the type definitions of the properties within actual implementations (e.g., `VariantBSucceedingSurrogate`)
**MUST NOT** override those of the base surrogate (`PolyBaseSurrogate`), this both applies to the type signature *and* nullability;
default values may be overridden. Type definitions (including nullability specification) affect the size of the serialized
variant, e.g., removal of a `?` from `Int?` for `VariantASucceedingSurrogate` in the example above will result in
a smaller size serialization (by 1 byte) than for `VariantBSucceedingSurrogate`.

Finally, implementation of serializers is derived via a minor configuration.
```kotlin
object VariantASucceedingSerializer : KSerializer<VariantA> by (
    SurrogateSerializer(VariantASucceedingSurrogate.serializer()) {
        VariantASucceedingSurrogate(myInt = it.myInt)
    })

object VariantBSucceedingSerializer : KSerializer<VariantB> by (
    SurrogateSerializer(VariantBSucceedingSurrogate.serializer()) {
        VariantBSucceedingSurrogate(myLong = it.myLong)
    })
```

This and a more realistic example can be found in [tests][1].

## Configuring serializers and running
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

[1]: src/test/kotlin/com/ing/serialization/bfl/serde/serializers/custom/polymorphic
[2]: src/test/kotlin/com/ing/serialization/bfl/serde/serializers/doc/ProjectGenericsTest.kt
[3]: src/test/kotlin/com/ing/serialization/bfl/serde/serializers/doc/ThirdPartyGenericsTest.kt
