package io.newm.ardrive.turbo.auth

import io.newm.ardrive.turbo.auth.model.SignatureHeaders

interface TurboSigner {
    val publicKey: ByteArray

    suspend fun sign(payload: ByteArray): ByteArray

    suspend fun generateSignatureHeaders(): SignatureHeaders
}
