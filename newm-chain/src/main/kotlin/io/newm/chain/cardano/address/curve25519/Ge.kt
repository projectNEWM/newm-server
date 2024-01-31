package io.newm.chain.cardano.address.curve25519

/**
 * Curve Group Element (Point)
 * The group element is using the extended homogeneous coordinates using (x,y,z,t)
 * which maps to coordinates using the following equations: X = x/z Y = y/z X * Y = t/z
 */
@OptIn(ExperimentalUnsignedTypes::class)
data class Ge(internal val x: Fe, internal val y: Fe, internal val z: Fe, internal val t: Fe) {
    operator fun plus(rhs: GePrecomp): GeP1P1 {
        val y1PlusX1 = y + x
        val y1MinusX1 = y - x
        val a = y1PlusX1 * rhs.yPlusX
        val b = y1MinusX1 * rhs.yMinusX
        val c = rhs.xy2d * t
        val d = z + z
        val x3 = a - b
        val y3 = a + b
        val z3 = d + c
        val t3 = d - c

        return GeP1P1(x3, y3, z3, t3)
    }

    operator fun plus(rhs: GeCached): GeP1P1 {
        val y1PlusX1 = y + x
        val y1MinusX1 = y - x
        val a = y1PlusX1 * rhs.yPlusX
        val b = y1MinusX1 * rhs.yMinusX
        val c = rhs.t2d * t
        val zz = z * rhs.z
        val d = zz + zz
        val x3 = a - b
        val y3 = a + b
        val z3 = d + c
        val t3 = d - c

        return GeP1P1(x3, y3, z3, t3)
    }

    fun doublePartial(): GePartial {
        return doubleP1P1().toPartial()
    }

    private fun doubleP1P1(): GeP1P1 {
        val xx = x.square()
        val yy = y.square()
        val b = z.squareAndDouble()
        val a = x + y
        val aa = a.square()
        val y3 = yy + xx
        val z3 = yy - xx
        val x3 = aa - y3
        val t3 = b - z3

        return GeP1P1(x3, y3, z3, t3)
    }

    /**
     * Flatten a group element on the affine plane (x,y)
     */
    fun toAffine(): GeAffine {
        val recip = z.invert()
        val x = this.x * recip
        val y = this.y * recip
        return GeAffine(x, y)
    }

    fun toBytes(): ByteArray {
        return toAffine().toBytes()
    }

    fun toCached(): GeCached {
        val yPlusX = y + x
        val yMinusX = y - x
        val t2d = t * Fe.D2
        return GeCached(yPlusX, yMinusX, z, t2d)
    }

    companion object {
        // The Identity Element for the group, which represent (X=0, Y=1) and is (x=0, y=1, z=1, t=0*1)
        private val ZERO: Ge = Ge(Fe.ZERO, Fe.ONE, Fe.ONE, Fe.ZERO)

        /**
         * Compute r = a * B
         * where
         * a = a[0]+2^8*a[1]+...+2^248 a[31] a scalar number represented by 32-bytes in little endian format
         * and a[31] is <= 0x80
         * B the ED25519 base point (not a parameter to the function)
         */
        fun scalarMultBase(a: Scalar64): Ge {
            // each es[i] is between 0 and 0xf
            // es[63] is between 0 and 7
            val es: ByteArray = a.nibbles()

            var carry: Byte = 0
            for (i in 0..62) {
                es[i] = (es[i] + carry).toByte()
                carry = (es[i] + 8).toByte()
                carry = (carry.toInt() shr 4).toByte()
                es[i] = (es[i] - (carry.toInt() shl 4).toByte()).toByte()
            }
            es[63] = (es[63] + carry).toByte()

            // each es[i] is between -8 and 8
            var h = ZERO
            for (j in 0..31) {
                val i = j * 2 + 1
                val t = GePrecomp.select(j, es[i])
                val r = h + t
                h = r.toFull()
            }
            h = h.doublePartial().double().double().doubleFull()

            for (j in 0..31) {
                val i = j * 2
                val t = GePrecomp.select(j, es[i])
                val r = h + t
                h = r.toFull()
            }

            return h
        }

        /**
         * Try to construct a group element (Point on the curve)
         * from its compressed byte representation (32 bytes little endian).
         * The compressed bytes representation is the y coordinate (255 bits)
         * and the sign of the x coordinate (1 bit) as the highest bit.
         */
        fun fromBytes(s: ByteArray): Ge? {
            return GeAffine.fromBytes(s)?.let { fromAffine(it) }
        }

        /**
         * Create a group element from affine coordinate
         */
        fun fromAffine(affine: GeAffine): Ge {
            val t = affine.x * affine.y
            return Ge(affine.x, affine.y, Fe.ONE, t)
        }
    }
}
