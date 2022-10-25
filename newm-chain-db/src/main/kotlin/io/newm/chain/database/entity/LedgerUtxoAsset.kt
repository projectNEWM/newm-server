package io.newm.chain.database.entity

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigInteger

@Serializable
data class LedgerUtxoAsset(
    val id: Long? = null,
    val ledgerUtxoId: Long,
    val ledgerAssetId: Long,
    @Contextual val amount: BigInteger,
)
