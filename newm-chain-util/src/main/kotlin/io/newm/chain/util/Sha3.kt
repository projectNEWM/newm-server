package io.newm.chain.util

import org.bouncycastle.crypto.digests.SHA3Digest
import kotlin.concurrent.getOrSet

object Sha3 {
    private val sha3_256Container = ThreadLocal<SHA3Digest>()

    /**
     * Sha3-256 hash
     */
    fun hash256(input: ByteArray): ByteArray {
        val sha3Digest = sha3_256Container.getOrSet { SHA3Digest(256) }
        sha3Digest.update(input, 0, input.size)
        val hash = ByteArray(32)
        sha3Digest.doFinal(hash, 0)
        return hash
    }
}
