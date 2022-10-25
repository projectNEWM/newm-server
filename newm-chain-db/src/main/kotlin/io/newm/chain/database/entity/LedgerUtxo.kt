package io.newm.chain.database.entity

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigInteger

@Serializable
data class LedgerUtxo(
    val id: Long? = null,
    val ledgerId: Long,
    val txId: String,
    val txIx: Int,
    @Contextual val lovelace: BigInteger,
    val blockCreated: Long,
    val slotCreated: Long,
    val blockSpent: Long?,
    val slotSpent: Long?,
    val transactionSpent: String?,
)
