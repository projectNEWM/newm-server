package io.newm.chain.cardano.address

import com.github.benmanes.caffeine.cache.Caffeine
import io.newm.chain.cardano.address.curve25519.Ge
import io.newm.chain.cardano.address.curve25519.Scalar64
import io.newm.chain.util.Bech32
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.getOrSet

class BIP32PublicKey(val bech32XPub: String) {
    val pk: ByteArray
    val chaincode: ByteArray

    init {
        require(bech32XPub.startsWith("xpub"))
        val xpubBytes = Bech32.decode(bech32XPub).bytes
        require(xpubBytes.size == 64)
        pk = xpubBytes.sliceArray(0..31)
        chaincode = xpubBytes.sliceArray(32..63)
    }

    fun derive(index: UInt): BIP32PublicKey {
        // key derivation is expensive. Grab it from cache if we can.
        publicKeyDerivationCache.getIfPresent("$bech32XPub/$index")?.let {
            return it
        }

        val chainCodeKeySpec = SecretKeySpec(chaincode, MAC_ALGORITHM)
        val zMac = zMacThreadLocal.getOrSet { Mac.getInstance(MAC_ALGORITHM) }
        zMac.init(chainCodeKeySpec)
        val iMac = iMacThreadLocal.getOrSet { Mac.getInstance(MAC_ALGORITHM) }
        iMac.init(chainCodeKeySpec)
        val seri = le32(index)

        // Do Soft Derivation
        zMac.update(0x2)
        zMac.update(pk)
        zMac.update(seri)
        iMac.update(0x3)
        iMac.update(pk)
        iMac.update(seri)

        val zout = zMac.doFinal()
        val zl = zout.sliceArray(0..31)
        // val _zr = zout.sliceArray(32..63) // unused

        // println("zl: ${zl.toHexString()}")
        // println("zr: ${zr.toHexString()}")

        val left = pointPlus(pk, pointOfTrunc28Mul8(zl))

        val iout = iMac.doFinal()
        val cc = iout.sliceArray(32..63)

        val out = ByteArray(XPUB_SIZE)
        mkXPub(out, left, cc)

        zMac.reset()
        iMac.reset()

        return BIP32PublicKey(Bech32.encode("xpub", out)).also {
            publicKeyDerivationCache.put("$bech32XPub/$index", it)
        }
    }

    private fun mkXPub(
        out: ByteArray,
        pk: ByteArray,
        cc: ByteArray
    ) {
        require(out.size == XPUB_SIZE) { "Invalid output length" }
        require(pk.size == 32) { "Invalid public key length" }
        require(cc.size == CHAIN_CODE_SIZE) { "Invalid chain code length" }

        pk.copyInto(out, 0, 0, 32)
        cc.copyInto(out, 32, 0, CHAIN_CODE_SIZE)
    }

    private fun le32(i: UInt): ByteArray {
        return byteArrayOf(
            i.toByte(),
            (i shr 8).toByte(),
            (i shr 16).toByte(),
            (i shr 24).toByte()
        )
    }

    private fun add28Mul8(
        x: ByteArray,
        y: ByteArray
    ): ByteArray {
        var carry: UShort = 0u
        val out = ByteArray(32)

        for (i in 0..27) {
            val r: UShort =
                ((x[i].toUByte().toUShort() + (y[i].toUByte().toUInt() shl 3).toUShort()).toUShort() + carry).toUShort()
            out[i] = (r and 0xffu).toByte()
            carry = (r.toUInt() shr 8).toUShort()
        }
        for (i in 28..31) {
            val r = (x[i].toUByte().toUShort() + carry).toUShort()
            out[i] = (r and 0xffu).toByte()
            carry = (r.toUInt() shr 8).toUShort()
        }
        return out
    }

    private fun pointOfTrunc28Mul8(sk: ByteArray): ByteArray {
        val copy = add28Mul8(BYTEARRAY_ZERO, sk)
        val scalar = Scalar64.fromBytes(copy)
        val a = Ge.scalarMultBase(scalar)
        return a.toBytes()
    }

    private fun pointPlus(
        p1: ByteArray,
        p2: ByteArray
    ): ByteArray {
        val a = requireNotNull(Ge.fromBytes(p1)) { "InvalidAddition" }
        val b = requireNotNull(Ge.fromBytes(p2)) { "InvalidAddition" }
        val r = a + b.toCached()
        val rBytes = r.toFull().toBytes()
        rBytes[31] = (rBytes[31].toUByte().toUInt() xor 0x80u).toByte()
        return rBytes
    }

    companion object {
        private const val MAC_ALGORITHM = "HmacSHA512"
        private const val XPUB_SIZE = 64
        private const val CHAIN_CODE_SIZE = 32
        private val BYTEARRAY_ZERO = ByteArray(32)
        private val zMacThreadLocal = ThreadLocal<Mac>()
        private val iMacThreadLocal = ThreadLocal<Mac>()

        private val publicKeyDerivationCache =
            Caffeine.newBuilder()
                .maximumSize(1073741824L) // 1GB - 2^30
                .build<String, BIP32PublicKey>()
    }
}
