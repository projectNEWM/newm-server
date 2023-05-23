package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParticipantX(
    @SerialName("participant_id")
    val participantId: Int,
    @SerialName("participant_name")
    val participantName: String,
    @SerialName("roles")
    val roles: List<RoleX>,
    @SerialName("payout_share_percentage")
    val payoutSharePercentage: Int
)
