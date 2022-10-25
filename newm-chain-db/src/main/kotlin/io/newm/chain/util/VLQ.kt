package io.newm.chain.util

import java.nio.ByteBuffer

/**
 * Decode pointer addresses into their parts
 * Variable-Length Quantity
 * https://en.wikipedia.org/wiki/Variable-length_quantity
 * https://rosettacode.org/wiki/Variable-length_quantity#Java
 */
object VLQ {
    fun decode(buffer: ByteBuffer): Long {
        require(buffer.hasRemaining()) { "Not enough bytes to decode VLQ!" }
        var n = 0L
        while (buffer.hasRemaining()) {
            val curByte: Int = buffer.get().toInt() and 0xFF
            n = (n shl 7) or (curByte and 0x7F).toLong()
            if (curByte and 0x80 == 0)
                break
        }
        return n
    }

    fun encode(n: Long): ByteArray {
        var number = n
        val numRelevantBits: Int = 64 - java.lang.Long.numberOfLeadingZeros(number)
        var numBytes: Int = (numRelevantBits + 6) / 7
        if (numBytes == 0) {
            numBytes = 1
        }
        val output = ByteArray(numBytes)
        for (i in numBytes - 1 downTo 0) {
            var curByte = (number and 0x7FL).toInt()
            if (i != (numBytes - 1)) {
                curByte = curByte or 0x80
            }
            output[i] = curByte.toByte()
            number = number ushr 7
        }
        return output
    }
}
