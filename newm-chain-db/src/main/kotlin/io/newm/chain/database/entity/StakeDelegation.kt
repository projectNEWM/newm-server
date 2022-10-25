package io.newm.chain.database.entity

data class StakeDelegation(
    val id: Long? = null,

    // the block in which this delegation/deregistration occurred
    val blockNumber: Long,

    // the user's stake address
    val stakeAddress: String,

    // the epoch when the user last dripped this token
    val epoch: Long,

    // the pool id the user delegated to. null if this is a de-registration of the stake address
    val poolId: String?,
) {
    val isRegistration: Boolean by lazy { poolId != null }
}
