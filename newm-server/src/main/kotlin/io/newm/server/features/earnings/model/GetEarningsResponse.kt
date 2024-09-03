package io.newm.server.features.earnings.model

import kotlinx.serialization.Serializable

@Serializable
data class GetEarningsResponse(
    val earnings: List<Earning>,
    val totalClaimed: Long
)
