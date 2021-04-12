# Serialization Format

## BigDecimals

BigDecimals are used to serialize `Float`, `Double` and `BigDecimal` types.

The encoded representation of a BigDecimal is a byte stream with the next elements in the following order:

- Byte: sign, -1, 0, or 1
- ByteArray for integer part, fixed size
  - Int: number of significant decimals in integer part
  - Byte[]: decimals of the integer part with little endian encoding
- ByteArray for fraction part, fixed size
  - Int: number of significant decimals in fraction part
  - Byte[]: decimals of the fraction part with big endian encoding

Note that the integer and fraction part are encoded with different endianness, this will be
explained in the section about [encoding](#encoding-and-endianness).

### Example

The size of the encoded BigDecimal is retrieved from the `@FixedLenght` annotation. So consider the following
definition:

```kotlin
@Serializable
data class Data(
  @FixedLength([6, 4])
  val value: @Contextual BigDecimal
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

See [BigDecimalDocTest.kt][1] for details.

### Encoding and Endianness

The rationale behind the difference in endianness of the integer and fraction part is mathematical consistency. The
whole byte array is now a valid representation of the part in the given endianness. This means that processors can
either work with the first `n` bytes, or just process the whole array of bytes independently of the length field.

Given the value in the [example](#example), processing the whole integer part with little-endianness will result in:
"000123", which equals "123".

And similarly for the fraction, processing the whole fraction part with big-endianness will result in ".4560", which
equals ".456".

[1]: src/test/kotlin/com/ing/serialization/bfl/serde/serializers/custom/BigDecimalDocTest.kt "BigDecimalDocTest"
