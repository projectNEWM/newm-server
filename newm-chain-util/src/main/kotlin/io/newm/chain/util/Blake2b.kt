package io.newm.chain.util

import org.bouncycastle.crypto.digests.Blake2bDigest
import kotlin.concurrent.getOrSet

object Blake2b {
    private val blake2b224Container = ThreadLocal<Blake2bDigest>()
    private val blake2b256Container = ThreadLocal<Blake2bDigest>()
    private val blake2b160Container = ThreadLocal<Blake2bDigest>()

    /**
     * Blake2b224 hash
     */
    fun hash224(input: ByteArray): ByteArray {
        val blake2b224 = blake2b224Container.getOrSet { Blake2bDigest(224) }
        blake2b224.update(input, 0, input.size)
        val hash = ByteArray(28)
        blake2b224.doFinal(hash, 0)
        return hash
    }

    /**
     * Blake2b256 hash
     */
    fun hash256(input: ByteArray): ByteArray {
        val blake2b256 = blake2b256Container.getOrSet { Blake2bDigest(256) }
        blake2b256.update(input, 0, input.size)
        val hash = ByteArray(32)
        blake2b256.doFinal(hash, 0)
        return hash
    }

    /**
     * Blake2b160 hash
     */
    fun hash160(input: ByteArray): ByteArray {
        val blake2b160 = blake2b160Container.getOrSet { Blake2bDigest(160) }
        blake2b160.update(input, 0, input.size)
        val hash = ByteArray(20)
        blake2b160.doFinal(hash, 0)
        return hash
    }
}
