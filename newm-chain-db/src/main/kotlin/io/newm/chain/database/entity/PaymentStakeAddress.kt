package io.newm.chain.database.entity

data class PaymentStakeAddress(
    val id: Long? = null,
    val receivingAddress: String,
    val stakeAddress: String,
)
