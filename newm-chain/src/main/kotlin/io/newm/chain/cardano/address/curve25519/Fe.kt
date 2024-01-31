package io.newm.chain.cardano.address.curve25519

import io.newm.chain.util.toBigInteger
import io.newm.chain.util.toLittleEndianBytes
import io.newm.chain.util.toULong

/**
 * Field Element implementation for 64-bits native arch using unsaturated 51-bits limbs.
 * arithmetic calculation helpers:
 * ed25519-donna: https://github.com/floodyberry/ed25519-donna
 * Sandy2x: New Curve25519 Speed Records
 */
@OptIn(ExperimentalUnsignedTypes::class)
data class Fe(private val values: ULongArray) {
    operator fun plus(rhs: Fe): Fe {
        val f0 = values[0]
        val f1 = values[1]
        val f2 = values[2]
        val f3 = values[3]
        val f4 = values[4]

        val g0 = rhs.values[0]
        val g1 = rhs.values[1]
        val g2 = rhs.values[2]
        val g3 = rhs.values[3]
        val g4 = rhs.values[4]

        var h0 = f0 + g0
        var c = h0 shr 51
        h0 = h0 and MASK
        var h1 = f1 + g1 + c
        c = h1 shr 51
        h1 = h1 and MASK
        var h2 = f2 + g2 + c
        c = h2 shr 51
        h2 = h2 and MASK
        var h3 = f3 + g3 + c
        c = h3 shr 51
        h3 = h3 and MASK
        var h4 = f4 + g4 + c
        c = h4 shr 51
        h4 = h4 and MASK
        h0 += c * 19u

        return Fe(ulongArrayOf(h0, h1, h2, h3, h4))
    }

    operator fun minus(rhs: Fe): Fe {
        val f0 = values[0]
        val f1 = values[1]
        val f2 = values[2]
        val f3 = values[3]
        val f4 = values[4]

        val g0 = rhs.values[0]
        val g1 = rhs.values[1]
        val g2 = rhs.values[2]
        val g3 = rhs.values[3]
        val g4 = rhs.values[4]

        var h0 = f0 + FOUR_P0 - g0
        var c = h0 shr 51
        h0 = h0 and MASK
        var h1 = f1 + FOUR_P1234 - g1 + c
        c = h1 shr 51
        h1 = h1 and MASK
        var h2 = f2 + FOUR_P1234 - g2 + c
        c = h2 shr 51
        h2 = h2 and MASK
        var h3 = f3 + FOUR_P1234 - g3 + c
        c = h3 shr 51
        h3 = h3 and MASK
        var h4 = f4 + FOUR_P1234 - g4 + c
        c = h4 shr 51
        h4 = h4 and MASK
        h0 += c * 19u

        return Fe(ulongArrayOf(h0, h1, h2, h3, h4))
    }

    operator fun unaryMinus(): Fe {
        val g0 = values[0]
        val g1 = values[1]
        val g2 = values[2]
        val g3 = values[3]
        val g4 = values[4]

        var h0 = FOUR_P0 - g0
        var c = h0 shr 51
        h0 = h0 and MASK
        var h1 = FOUR_P1234 - g1 + c
        c = h1 shr 51
        h1 = h1 and MASK
        var h2 = FOUR_P1234 - g2 + c
        c = h2 shr 51
        h2 = h2 and MASK
        var h3 = FOUR_P1234 - g3 + c
        c = h3 shr 51
        h3 = h3 and MASK
        var h4 = FOUR_P1234 - g4 + c
        c = h4 shr 51
        h4 = h4 and MASK
        h0 += c * 19u

        return Fe(ulongArrayOf(h0, h1, h2, h3, h4))
    }

    operator fun times(rhs: Fe): Fe {
        var r0 = values[0]
        var r1 = values[1]
        var r2 = values[2]
        var r3 = values[3]
        var r4 = values[4]

        val s0 = rhs.values[0]
        val s1 = rhs.values[1]
        val s2 = rhs.values[2]
        val s3 = rhs.values[3]
        val s4 = rhs.values[4]

        var t0 = mul128(r0, s0)
        var t1 = mul128(r0, s1) + mul128(r1, s0)
        var t2 = mul128(r0, s2) + mul128(r2, s0) + mul128(r1, s1)
        var t3 = mul128(r0, s3) + mul128(r3, s0) + mul128(r1, s2) + mul128(r2, s1)
        var t4 = mul128(r0, s4) + mul128(r4, s0) + mul128(r3, s1) + mul128(r1, s3) + mul128(r2, s2)

        r1 *= 19uL
        r2 *= 19uL
        r3 *= 19uL
        r4 *= 19uL

        t0 += mul128(r4, s1) + mul128(r1, s4) + mul128(r2, s3) + mul128(r3, s2)
        t1 += mul128(r4, s2) + mul128(r2, s4) + mul128(r3, s3)
        t2 += mul128(r4, s3) + mul128(r3, s4)
        t3 += mul128(r4, s4)

        r0 = t0.toULong() and MASK
        var c = (t0 shr 51).toULong()
        t1 += c.toBigInteger()
        r1 = t1.toULong() and MASK
        c = (t1 shr 51).toULong()
        t2 += c.toBigInteger()
        r2 = t2.toULong() and MASK
        c = (t2 shr 51).toULong()
        t3 += c.toBigInteger()
        r3 = t3.toULong() and MASK
        c = (t3 shr 51).toULong()
        t4 += c.toBigInteger()
        r4 = t4.toULong() and MASK
        c = (t4 shr 51).toULong()
        r0 += c * 19uL
        c = r0 shr 51
        r0 = r0 and MASK

        r1 += c

        return Fe(ulongArrayOf(r0, r1, r2, r3, r4))
    }

    /**
     * Compute the square of the field element
     */
    fun square(): Fe {
        var r0 = values[0]
        var r1 = values[1]
        var r2 = values[2]
        var r3 = values[3]
        var r4 = values[4]

        val d0 = r0 * 2u
        val d1 = r1 * 2u
        val d2 = r2 * 2u * 19u
        val d419 = r4 * 19u
        val d4 = d419 * 2u

        val t0 = mul128(r0, r0) + mul128(d4, r1) + mul128(d2, r3)
        val t1 = mul128(d0, r1) + mul128(d4, r2) + mul128(r3, r3 * 19u)
        val t2 = mul128(d0, r2) + mul128(r1, r1) + mul128(d4, r3)
        val t3 = mul128(d0, r3) + mul128(d1, r2) + mul128(r4, d419)
        val t4 = mul128(d0, r4) + mul128(d1, r3) + mul128(r2, r2)

        r0 = t0.toULong() and MASK
        r1 = t1.toULong() and MASK
        var c = shl128(t0, 13)
        r1 += c
        r2 = t2.toULong() and MASK
        c = shl128(t1, 13)
        r2 += c
        r3 = t3.toULong() and MASK
        c = shl128(t2, 13)
        r3 += c
        r4 = t4.toULong() and MASK
        c = shl128(t3, 13)
        r4 += c
        c = shl128(t4, 13)
        r0 += c * 19u
        c = r0 shr 51
        r0 = r0 and MASK
        r1 += c
        c = r1 shr 51
        r1 = r1 and MASK
        r2 += c
        c = r2 shr 51
        r2 = r2 and MASK
        r3 += c
        c = r3 shr 51
        r3 = r3 and MASK
        r4 += c
        c = r4 shr 51
        r4 = r4 and MASK
        r0 += c * 19u

        return Fe(ulongArrayOf(r0, r1, r2, r3, r4))
    }

    /**
     * Compute the (2^N) square of the field element
     * This is performed by repeated squaring of the element
     */
    fun squareRepeatdly(n: Int): Fe {
        var r0 = this.values[0]
        var r1 = this.values[1]
        var r2 = this.values[2]
        var r3 = this.values[3]
        var r4 = this.values[4]

        repeat(n) {
            val d0 = r0 * 2u
            val d1 = r1 * 2u
            val d2 = r2 * 2u * 19u
            val d419 = r4 * 19u
            val d4 = d419 * 2u

            val t0 = mul128(r0, r0) + mul128(d4, r1) + mul128(d2, r3)
            val t1 = mul128(d0, r1) + mul128(d4, r2) + mul128(r3, r3 * 19u)
            val t2 = mul128(d0, r2) + mul128(r1, r1) + mul128(d4, r3)
            val t3 = mul128(d0, r3) + mul128(d1, r2) + mul128(r4, d419)
            val t4 = mul128(d0, r4) + mul128(d1, r3) + mul128(r2, r2)

            r0 = t0.toULong() and MASK
            r1 = t1.toULong() and MASK
            var c = shl128(t0, 13)
            r1 += c
            r2 = t2.toULong() and MASK
            c = shl128(t1, 13)
            r2 += c
            r3 = t3.toULong() and MASK
            c = shl128(t2, 13)
            r3 += c
            r4 = t4.toULong() and MASK
            c = shl128(t3, 13)
            r4 += c
            c = shl128(t4, 13)
            r0 += c * 19u

            c = r0 shr 51
            r0 = r0 and MASK
            r1 += c
            c = r1 shr 51
            r1 = r1 and MASK
            r2 += c
            c = r2 shr 51
            r2 = r2 and MASK
            r3 += c
            c = r3 shr 51
            r3 = r3 and MASK
            r4 += c
            c = r4 shr 51
            r4 = r4 and MASK
            r0 += c * 19u
        }

        return Fe(ulongArrayOf(r0, r1, r2, r3, r4))
    }

    /**
     * Compute the square of the element and returns its double
     * this is more efficient than squaring and adding the result together
     */
    fun squareAndDouble(): Fe {
        val x = this.square()
        for (i in 0..4) {
            x.values[i] = x.values[i] * 2u
        }
        return x
    }

    /**
     * Calculate the invert of the Field element
     *
     * the element to invert must be non 0
     */
    @Suppress("ktlint:standard:property-naming")
    fun invert(): Fe {
        val z1 = this
        val z2 = z1.square()
        val z8 = z2.squareRepeatdly(2)
        val z9 = z1 * z8
        val z11 = z2 * z9
        val z22 = z11.square()
        val z_5_0 = z9 * z22
        val z_10_5 = z_5_0.squareRepeatdly(5)
        val z_10_0 = z_10_5 * z_5_0
        val z_20_10 = z_10_0.squareRepeatdly(10)
        val z_20_0 = z_20_10 * z_10_0
        val z_40_20 = z_20_0.squareRepeatdly(20)
        val z_40_0 = z_40_20 * z_20_0
        val z_50_10 = z_40_0.squareRepeatdly(10)
        val z_50_0 = z_50_10 * z_10_0
        val z_100_50 = z_50_0.squareRepeatdly(50)
        val z_100_0 = z_100_50 * z_50_0
        val z_200_100 = z_100_0.squareRepeatdly(100)
        val z_200_0 = z_200_100 * z_100_0
        val z_250_50 = z_200_0.squareRepeatdly(50)
        val z_250_0 = z_250_50 * z_50_0
        val z_255_5 = z_250_0.squareRepeatdly(5)
        val z_255_21 = z_255_5 * z11

        return z_255_21
    }

    @Suppress("ktlint:standard:property-naming")
    fun pow25523(): Fe {
        val z2 = this.square()
        val z8 = z2.squareRepeatdly(2)
        val z9 = this * z8
        val z11 = z2 * z9
        val z22 = z11.square()
        val z_5_0 = z9 * z22
        val z_10_5 = z_5_0.squareRepeatdly(5)
        val z_10_0 = z_10_5 * z_5_0
        val z_20_10 = z_10_0.squareRepeatdly(10)
        val z_20_0 = z_20_10 * z_10_0
        val z_40_20 = z_20_0.squareRepeatdly(20)
        val z_40_0 = z_40_20 * z_20_0
        val z_50_10 = z_40_0.squareRepeatdly(10)
        val z_50_0 = z_50_10 * z_10_0
        val z_100_50 = z_50_0.squareRepeatdly(50)
        val z_100_0 = z_100_50 * z_50_0
        val z_200_100 = z_100_0.squareRepeatdly(100)
        val z_200_0 = z_200_100 * z_100_0
        val z_250_50 = z_200_0.squareRepeatdly(50)
        val z_250_0 = z_250_50 * z_50_0
        val z_252_2 = z_250_0.squareRepeatdly(2)
        val z_252_3 = z_252_2 * this

        return z_252_3
    }

    private fun carryFull(t: ULongArray): ULongArray {
        val t1 = t[1] + (t[0] shr 51)
        val t2 = t[2] + (t1 shr 51)
        val t3 = t[3] + (t2 shr 51)
        val t4 = t[4] + (t3 shr 51)
        val t0 = (t[0] and MASK) + 19u * (t4 shr 51)
        return ulongArrayOf(t0, t1 and MASK, t2 and MASK, t3 and MASK, t4 and MASK)
    }

    private fun carryFinal(t: ULongArray): ULongArray {
        val t1 = t[1] + (t[0] shr 51)
        val t2 = t[2] + (t1 shr 51)
        val t3 = t[3] + (t2 shr 51)
        val t4 = t[4] + (t3 shr 51)
        return ulongArrayOf(t[0] and MASK, t1 and MASK, t2 and MASK, t3 and MASK, t4 and MASK)
    }

    private fun toPacked(): ULongArray {
        var t = carryFull(values)
        t = carryFull(t)
        t[0] = t[0] + 19u
        t = carryFull(t)
        t[0] += (MASK + 1u) - 19u
        t[1] += MASK
        t[2] += MASK
        t[3] += MASK
        t[4] += MASK
        t = carryFinal(t)
        val out0 = t[0] or (t[1] shl 51)
        val out1 = (t[1] shr 13) or (t[2] shl 38)
        val out2 = (t[2] shr 26) or (t[3] shl 25)
        val out3 = (t[3] shr 39) or (t[4] shl 12)
        return ulongArrayOf(out0, out1, out2, out3)
    }

    private fun write8(
        ofs: Int,
        v: ULong,
        out: ByteArray
    ) {
        val x = v.toLittleEndianBytes()
        out[ofs] = x[0]
        out[ofs + 1] = x[1]
        out[ofs + 2] = x[2]
        out[ofs + 3] = x[3]
        out[ofs + 4] = x[4]
        out[ofs + 5] = x[5]
        out[ofs + 6] = x[6]
        out[ofs + 7] = x[7]
    }

    /**
     * Represent the Field Element as little-endian canonical bytes (256 bits)
     * Due to the field size, it's guaranteed that the highest bit is always 0
     */
    fun toBytes(): ByteArray {
        val out = ByteArray(32)
        val packed = this.toPacked()
        write8(0, packed[0], out)
        write8(8, packed[1], out)
        write8(16, packed[2], out)
        write8(24, packed[3], out)
        return out
    }

    fun isNegative(): Boolean {
        return (this.toPacked()[0] and 1uL) != 0uL
    }

    fun isNonZero(): Boolean {
        return !ZERO_BYTES.contentEquals(this.toBytes())
    }

    companion object {
        // mask
        // const MASK: u64 = (1 << 51) - 1;
        private val MASK: ULong = 0x7ffffffffffffuL // (1uL shl 51) - 1uL

        // multiple of P
        private const val FOUR_P0: ULong = 0x1fffffffffffb4uL
        private const val FOUR_P1234: ULong = 0x1ffffffffffffcuL

        private val ZERO_BYTES = ByteArray(32)
        val ZERO: Fe = Fe(ulongArrayOf(0uL, 0uL, 0uL, 0uL, 0uL))
        val ONE: Fe = Fe(ulongArrayOf(1uL, 0uL, 0uL, 0uL, 0uL))

        val SQRTM1: Fe =
            Fe(
                ulongArrayOf(
                    0x61b274a0ea0b0uL,
                    0xd5a5fc8f189duL,
                    0x7ef5e9cbd0c60uL,
                    0x78595a6804c9euL,
                    0x2b8324804fc1duL
                )
            )

        val D: Fe =
            Fe(
                ulongArrayOf(
                    0x34dca135978a3uL,
                    0x1a8283b156ebduL,
                    0x5e7a26001c029uL,
                    0x739c663a03cbbuL,
                    0x52036cee2b6ffuL
                )
            )

        val D2: Fe =
            Fe(
                ulongArrayOf(
                    0x69b9426b2f159uL,
                    0x35050762add7auL,
                    0x3cf44c0038052uL,
                    0x6738cc7407977uL,
                    0x2406d9dc56dffuL
                )
            )

        /**
         * Create the Field Element from its little-endian byte representation (256 bits)
         * Note that it doesn't verify that the bytes
         * are actually representing an element in the
         * range of the field, but will automatically wrap
         * the bytes to be in the range
         */
        fun fromBytes(bytes: ByteArray): Fe {
            // maps from bytes at:
            // * bit 0 (byte 0 shift 0)
            // * bit 51 (byte 6 shift 3)
            // * bit 102 (byte 12 shift 6)
            // * bit 153 (byte 19 shift 1)
            // * bit 204 (byte 25 shift 4)
            val x0 = load(bytes, 0) and MASK
            val x1 = (load(bytes, 6) shr 3) and MASK
            val x2 = (load(bytes, 12) shr 6) and MASK
            val x3 = (load(bytes, 19) shr 1) and MASK
            val x4 = (load(bytes, 24) shr 12) and MASK
            return Fe(ulongArrayOf(x0, x1, x2, x3, x4))
        }
    }
}
