package io.newm.ardrive.turbo.payment

import io.newm.ardrive.turbo.auth.ArweaveSigner

interface TokenTools {
    suspend fun createAndSubmitTx(params: TokenCreateTxParams): TokenCreateTxResult

    suspend fun pollTxAvailability(txId: String)
}

data class TokenCreateTxParams(
    val target: String,
    val tokenAmount: String,
    val feeMultiplier: Double,
    val signer: ArweaveSigner,
    val turboCreditDestinationAddress: String? = null,
)

data class TokenCreateTxResult(
    val id: String,
    val target: String,
    val reward: String? = null,
)
