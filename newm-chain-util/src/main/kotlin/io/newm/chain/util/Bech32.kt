package io.newm.chain.util

object Bech32 {
    private const val ENCODING_CONST = 1
    private const val BECH32_ALPHABET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
    private val BECH32_ALPHABET_MAP: Map<Char, Int> =
        mapOf(*BECH32_ALPHABET.mapIndexed { index, char -> Pair(char, index) }.toTypedArray())

    private fun performPolymodStep(pre: Int): Int {
        val b = pre shr 25
        return ((pre and 0x1ffffff) shl 5) xor
            (-((b shr 0) and 1) and 0x3b6a57b2) xor
            (-((b shr 1) and 1) and 0x26508e6d) xor
            (-((b shr 2) and 1) and 0x1ea119fa) xor
            (-((b shr 3) and 1) and 0x3d4233dd) xor
            (-((b shr 4) and 1) and 0x2a1462b3)
    }

    private fun prefixCheck(prefix: String): Int {
        var chk = 1
        val charCodes = prefix.map { char -> char.code }
        charCodes.forEach { c ->
            if (c < 33 || c > 126) {
                throw IllegalArgumentException("Invalid prefix ($prefix)")
            }
            chk = performPolymodStep(chk) xor (c shr 5)
        }
        chk = performPolymodStep(chk)

        charCodes.forEach { v ->
            chk = performPolymodStep(chk) xor (v and 0x1f)
        }
        return chk
    }

    private fun convert(
        data: IntArray,
        inBits: Int,
        outBits: Int,
        pad: Boolean
    ): IntArray {
        var value = 0
        var bits = 0
        val maxV = (1 shl outBits) - 1

        val result = mutableListOf<Int>()
        data.forEach { chunk ->
            value = (value shl inBits) or chunk
            bits += inBits

            while (bits >= outBits) {
                bits -= outBits
                result.add((value shr bits) and maxV)
            }
        }

        if (pad) {
            if (bits > 0) {
                result.add((value shl (outBits - bits)) and maxV)
            }
        } else {
            if (bits >= inBits) {
                throw IllegalArgumentException("Excess padding")
            }
            if ((value shl (outBits - bits)) and maxV > 0) {
                throw IllegalArgumentException("Non-zero padding")
            }
        }

        return result.toIntArray()
    }

    private fun toWords(bytes: ByteArray): IntArray {
        val bytesToWordArray = bytes.map { it.toInt() and 0xff }.toIntArray()
        return convert(bytesToWordArray, 8, 5, true)
    }

    private fun fromWords(words: IntArray): ByteArray {
        val res = convert(words, 5, 8, false)
        return res.map { it.toByte() }.toByteArray()
    }

    fun encode(
        prefix: String,
        bytes: ByteArray
    ): String {
        val words = toWords(bytes)
        val pre = prefix.lowercase()
        var chk = prefixCheck(pre)

        var result = "${pre}1"
        words.forEach { x ->
            if (x shr 5 != 0) {
                throw IllegalArgumentException("Non 5-bit word")
            }

            chk = performPolymodStep(chk) xor x
            result += BECH32_ALPHABET[x]
        }

        repeat(6) {
            chk = performPolymodStep(chk)
        }
        chk = chk xor ENCODING_CONST

        repeat(6) { i ->
            val v = (chk shr ((5 - i) * 5)) and 0x1f
            result += BECH32_ALPHABET[v]
        }

        return result
    }

    fun decode(bech32Str: String): Decoded {
        if (bech32Str.length < 8) {
            throw IllegalArgumentException("bech32 string too short")
        }

        val lowered = bech32Str.lowercase()
        val uppered = bech32Str.uppercase()
        if (bech32Str !== lowered && bech32Str !== uppered) {
            throw IllegalArgumentException("Mixed-case string!")
        }
        val str = lowered

        val split = str.lastIndexOf('1')
        if (split == -1) {
            throw IllegalArgumentException("No separator character for $str")
        }
        if (split == 0) {
            throw IllegalArgumentException("Missing prefix for $str")
        }

        val prefix = str.substringBeforeLast('1')
        val wordChars = str.substringAfterLast('1')
        if (wordChars.length < 6) {
            throw IllegalArgumentException("Data too short!")
        }

        var chk = prefixCheck(prefix)
        val words = mutableListOf<Int>()
        wordChars.forEachIndexed { i, c ->
            val v = BECH32_ALPHABET_MAP[c]
            v?.let {
                chk = performPolymodStep(chk) xor v

                if (i + 6 >= wordChars.length) {
                    return@forEachIndexed
                }
                words.add(v)
            } ?: throw IllegalArgumentException("Unknown character $c")
        }

        if (chk != ENCODING_CONST) {
            throw IllegalArgumentException("Invalid checksum for $str")
        }
        return Decoded(prefix, fromWords(words.toIntArray()))
    }

    data class Decoded(
        val prefix: String,
        val bytes: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Decoded

            if (prefix != other.prefix) return false
            if (!bytes.contentEquals(other.bytes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = prefix.hashCode()
            result = 31 * result + bytes.contentHashCode()
            return result
        }
    }
}
