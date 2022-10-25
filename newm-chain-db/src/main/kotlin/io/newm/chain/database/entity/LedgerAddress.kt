package io.newm.chain.database.entity

import kotlinx.serialization.Serializable

@Serializable
data class LedgerAddress(
    val id: Long? = null,
    val address: String,
    val stakeAddress: String?,
    val addressType: String,
)
