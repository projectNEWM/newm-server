package io.newm.chain.database.entity

data class TransactionLog(
    val id: Long? = null,
    val transactionId: String,
    val cbor: String,
    val mintingPolicy: String?,
    val timestamp: String,
    val result: String,
)
