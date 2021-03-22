BFLS format for [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization).

![build status](https://github.com/ingzkp/kotlinx-serialization-bfl/actions/workflows/build/badge.svg)

# BFLS

Binary fixed length serialization (BFLS) protocol is dedicated to provide to particular data type the binary
representation of fixed length. It is achieved by specifying the max length of each structure of variable length (i.e.
List, Map, String, etc) and padding bytes in the end.

Protocol supports the following data types:

* bool
* byte
* short
* integer
* long
* char
* all their compound types

# Examples

#### String

```kotlin
@Serializable
data class DataClassWithString(@FixedLength([20])val name: String, val sugar: Int)
```

If class has string of length less than 20, it will write its length to short variable, then will write each string
character, and finally add two zero bytes for each "unused" characters, and then write `sugar` as a regular integer.
This protocol does not do variable-length encoding as Protobuf, thus, all variables of the same type have the same
length.

If class has a string longer than 20, the serialization will fail with error.

#### List

```kotlin
@Serializable
data class DataClassWithList(@FixedLength([42])val name: List<Int>, val sugar: Int)
```

Sizing logic is the same as in previous example, but the length of list is stored as an integer, but short.

#### Map

```kotlin
@Serializable
data class DataClassWithList(@FixedLength([20, 10])val name: Map<String, Int>, val sugar: Int)
```

`@FixedLength` has two parameters. First - `20` - is a length of Map itself, number of key-value pairs that should be
stored in it. Second - `10` - is a length of string key. Basically, you should declare lengths in order of
serialization. Map will be serialized firstly (obviously), then its first key, then its first value, etc.

Map can store any types, their length will be computed recursively and used to calculate the number of padding bytes.
