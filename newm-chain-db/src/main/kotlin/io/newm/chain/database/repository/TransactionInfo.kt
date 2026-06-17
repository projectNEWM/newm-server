package io.newm.chain.database.repository

import io.newm.chain.model.Utxo

data class TransactionInfo(
    val blockNumber: Long,
    val slotNumber: Long,
    val spentUtxos: List<Utxo>,
    val createdUtxos: List<Utxo>,
)

class TransactionInfoIntegrityException(
    message: String,
) : RuntimeException(message)
