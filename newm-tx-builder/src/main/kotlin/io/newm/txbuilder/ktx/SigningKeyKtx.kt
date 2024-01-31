package io.newm.txbuilder.ktx

import io.newm.chain.grpc.SigningKey
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import kotlin.concurrent.getOrSet

private val ed25519Signer = ThreadLocal<Ed25519Signer>()

fun SigningKey.sign(transactionId: ByteArray): ByteArray {
    val signer = ed25519Signer.getOrSet { Ed25519Signer() }
    signer.reset()
    signer.init(true, Ed25519PrivateKeyParameters(this.skey.toByteArray()))
    signer.update(transactionId, 0, transactionId.size)
    return signer.generateSignature()
}
