package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Participant(
    @SerialName("id")
    val id: Long,
    @SerialName("role_id")
    val roleId: List<Long>,
    // should always be 100 for the NEWM Participant
    @SerialName("payout_share_percentage")
    val payoutSharePercentage: Int
)
