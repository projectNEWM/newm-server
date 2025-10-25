package io.newm.txbuilder.ktx

import io.newm.chain.grpc.SigningKey
import kotlin.concurrent.getOrSet
import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer

private val ed25519Signer = ThreadLocal<Ed25519Signer>()
private val ed25519ExtendedSigner = ThreadLocal<EdDSAEngine>()

fun SigningKey.sign(transactionId: ByteArray): ByteArray =
    when (this.skey.size()) {
        32 -> {
            // sign with non-extended ed25519 key
            val signer = ed25519Signer.getOrSet { Ed25519Signer() }
            signer.reset()
            signer.init(true, Ed25519PrivateKeyParameters(this.skey.toByteArray()))
            signer.update(transactionId, 0, transactionId.size)
            signer.generateSignature()
        }
        64, 128 -> {
            // sign with extended ed25519 key (64 bytes) or ed25519e_bip32 key (128 bytes)
            // For 128 byte keys, use the first 64 bytes as the extended private key
            val keyBytes = this.skey.toByteArray()
            val extendedKeyBytes = if (keyBytes.size == 128) keyBytes.copyOfRange(0, 64) else keyBytes
            val ed25519ParameterSpec: EdDSAParameterSpec = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)
            val skSpec = EdDSAPrivateKeySpec(ed25519ParameterSpec, extendedKeyBytes)
            val sk = EdDSAPrivateKey(skSpec)
            val engine = ed25519ExtendedSigner.getOrSet { EdDSAEngine() }
            engine.initSign(sk)
            engine.signOneShot(transactionId)
        }
        else -> throw IllegalArgumentException("Invalid signing key size: ${this.skey.size()}")
    }

fun SigningKey.verify(
    transactionId: ByteArray,
    signature: ByteArray
): Boolean {
    val signer = ed25519Signer.getOrSet { Ed25519Signer() }
    signer.reset()
    signer.init(false, Ed25519PublicKeyParameters(this.vkey.toByteArray()))
    signer.update(transactionId, 0, transactionId.size)
    return signer.verifySignature(signature)
}
