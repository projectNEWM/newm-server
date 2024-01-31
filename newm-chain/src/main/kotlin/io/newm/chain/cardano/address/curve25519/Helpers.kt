package io.newm.chain.cardano.address.curve25519

import io.newm.chain.util.toBigInteger
import java.math.BigInteger

/**
 * load 8 bytes from input[ofs..ofs+7] as little endian u64
 */
@OptIn(ExperimentalUnsignedTypes::class)
internal fun load(
    bytes: ByteArray,
    ofs: Int
): ULong {
    require(bytes.size == 32) { "Invalid byte array length" }
    return (bytes[ofs].toUByte().toULong())
        .or((bytes[ofs + 1].toUByte().toULong() shl 8))
        .or((bytes[ofs + 2].toUByte().toULong() shl 16))
        .or((bytes[ofs + 3].toUByte().toULong() shl 24))
        .or((bytes[ofs + 4].toUByte().toULong() shl 32))
        .or((bytes[ofs + 5].toUByte().toULong() shl 40))
        .or((bytes[ofs + 6].toUByte().toULong() shl 48))
        .or((bytes[ofs + 7].toUByte().toULong() shl 56))
}

internal fun mul128(
    a: ULong,
    b: ULong
): BigInteger {
    return a.toBigInteger() * b.toBigInteger()
}

internal fun shl128(
    v: BigInteger,
    shift: Int
): ULong {
    return ((v shl shift) shr 64).toString(16).toULong(16)
}
