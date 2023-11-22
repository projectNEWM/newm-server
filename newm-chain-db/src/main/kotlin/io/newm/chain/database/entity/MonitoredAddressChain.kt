package io.newm.chain.database.entity

data class MonitoredAddressChain(
    val id: Long? = null,
    val address: String,
    val height: Long,
    val slot: Long,
    val hash: String,
)
