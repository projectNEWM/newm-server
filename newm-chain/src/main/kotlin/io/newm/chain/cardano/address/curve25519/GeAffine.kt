package io.newm.chain.cardano.address.curve25519

class GeAffine(internal val x: Fe, internal val y: Fe) {
    fun toBytes(): ByteArray {
        val bs = y.toBytes()
        bs[31] = (bs[31].toInt() xor (if (x.isNegative()) 1 else 0).shl(7)).toByte()
        return bs
    }

    companion object {
        fun fromBytes(s: ByteArray): GeAffine? {
            // See RFC8032 5.3.1 decoding process
            //
            // y (255 bits) | sign(x) (1 bit) = s
            // let u = y^2 - 1
            //     v = d * y^2 + 1
            //     x = u * v^3 * (u * v^7)^((p-5)/8)

            // recover y by clearing the highest bit (side effect of from_bytes)
            val y = Fe.fromBytes(s)

            // recover x
            val y2 = y.square()
            val u = y2 - Fe.ONE
            val v = y2 * Fe.D + Fe.ONE
            val v3 = v.square() * v
            val v7 = v3.square() * v
            val uv7 = v7 * u

            var x = uv7.pow25523() * v3 * u

            val vxx = x.square() * v
            val check = vxx - u
            if (check.isNonZero()) {
                val check2 = vxx + u
                if (check2.isNonZero()) {
                    return null
                }
                x *= Fe.SQRTM1
            }

            if (x.isNegative() == (s[31].toUByte().toUInt() shr 7 != 0u)) {
                x = -x
            }

            return GeAffine(x, y)
        }
    }
}
