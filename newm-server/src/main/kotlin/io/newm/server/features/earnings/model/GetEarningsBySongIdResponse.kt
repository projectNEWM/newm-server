package io.newm.server.features.earnings.model

import kotlinx.serialization.Serializable

@Serializable
data class GetEarningsBySongIdResponse(
    val earnings: List<Earning>,
    val totalAmount: Long,
)
