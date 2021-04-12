# Serialization Format

## BigDecimals

BigDecimals are used to serialize `Float`, `Double` and `BigDecimal` types.

The encoded representation of a BigDecimal is a byte stream with the following element in the following order:

- Byte: sign, -1, 0, or 1
- ByteArray for integer part, fixed size
  - Int: number of significant decimals in integer part
  - Byte[]: decimals of the integer part with little endian encoding
- ByteArray for fraction part, fixed size
  - Int: number of significant decimals in fraction part
  - Byte[]: decimals of the fraction part with big endian encoding

The size of the encoded BigDecimal is retrieved from the `@FixedLenght` annotation. So consider the following
definition:

```kotlin
@Serializable
data class Data(
  @FixedLenght([6, 4])
  val value: BigDecimal
)
```

Then the number "123.456" is encoded as:

    [1, 0, 0, 0, 3, 3, 2, 1, 0, 0, 0, 0, 0, 0, 3, 4, 5, 6, 0]
    \_/\__________/\________________/\__________/\__________/
     |      |             |               |           |- fraction: "456"
     |      |             |               |- fraction length: 3
     |      |             |- integer: "123", note the little-endian encoding
     |      |- integer length: 3
     |- 1, positive number

