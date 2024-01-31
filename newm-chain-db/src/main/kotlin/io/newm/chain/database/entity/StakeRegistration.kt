package io.newm.chain.database.entity

data class StakeRegistration(
    val id: Long? = null,
    // the user's stake address
    val stakeAddress: String,
    // the slot where the stake key registration happened
    val slot: Long,
    // the transaction index inside the block where the stake key registration happened
    val txIndex: Int,
    // the certificate index inside the transaction where the stake key registration happened
    val certIndex: Int,
)
