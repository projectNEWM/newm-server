package io.newm.chain.database.entity

import kotlinx.serialization.Serializable

@Serializable
data class LedgerUtxoHistory(
    val paymentCred: String,
    val stakeCred: String?,
    val txId: String,
    val block: Long,
)
