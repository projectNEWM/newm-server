package io.newm.chain.cardano.address.curve25519

@OptIn(ExperimentalUnsignedTypes::class)
data class Scalar64(private val values: ULongArray) {

    /**
     * Get the scalar in a form of 64 nibbles
     * nibble is a group of 4-bits
     */
    fun nibbles(): ByteArray {
        val es = ByteArray(64)

        // contract limbs
        val c = ULongArray(4)
        c[0] = (values[1] shl 56) or values[0]
        c[1] = (values[2] shl 48) or (values[1] shr 8)
        c[2] = (values[3] shl 40) or (values[2] shr 16)
        c[3] = (values[4] shl 32) or (values[3] shr 24)

        // write 16 nibbles for each saturated limbs, for 64 nibbles
        for (b in 0..3) {
            es[16 * b + 0] = ((c[b] shr 0) and 0b1111u).toByte()
            es[16 * b + 1] = ((c[b] shr 4) and 0b1111u).toByte()
            es[16 * b + 2] = ((c[b] shr 8) and 0b1111u).toByte()
            es[16 * b + 3] = ((c[b] shr 12) and 0b1111u).toByte()
            es[16 * b + 4] = ((c[b] shr 16) and 0b1111u).toByte()
            es[16 * b + 5] = ((c[b] shr 20) and 0b1111u).toByte()
            es[16 * b + 6] = ((c[b] shr 24) and 0b1111u).toByte()
            es[16 * b + 7] = ((c[b] shr 28) and 0b1111u).toByte()
            es[16 * b + 8] = ((c[b] shr 32) and 0b1111u).toByte()
            es[16 * b + 9] = ((c[b] shr 36) and 0b1111u).toByte()
            es[16 * b + 10] = ((c[b] shr 40) and 0b1111u).toByte()
            es[16 * b + 11] = ((c[b] shr 44) and 0b1111u).toByte()
            es[16 * b + 12] = ((c[b] shr 48) and 0b1111u).toByte()
            es[16 * b + 13] = ((c[b] shr 52) and 0b1111u).toByte()
            es[16 * b + 14] = ((c[b] shr 56) and 0b1111u).toByte()
            es[16 * b + 15] = ((c[b] shr 60) and 0b1111u).toByte()
        }
        return es
    }

    companion object {
        private const val MASK56 = 0x00FF_FFFF_FFFF_FFFFu

        @JvmStatic
        fun fromBytes(bytes: ByteArray): Scalar64 {
            val x0 = load(bytes, 0)
            val x1 = load(bytes, 8)
            val x2 = load(bytes, 16)
            val x3 = load(bytes, 24)

            val out0 = x0 and MASK56
            val out1 = ((x0 shr 56) or (x1 shl 8)) and MASK56
            val out2 = ((x1 shr 48) or (x2 shl 16)) and MASK56
            val out3 = ((x2 shr 40) or (x3 shl 24)) and MASK56
            val out4 = x3 shr 32
            return Scalar64(ulongArrayOf(out0, out1, out2, out3, out4))
        }
    }
}
