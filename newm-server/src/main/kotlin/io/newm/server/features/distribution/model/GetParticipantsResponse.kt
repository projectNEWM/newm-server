package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GetParticipantsResponse(
    @SerialName("message")
    val message: String,
    @SerialName("success")
    val success: Boolean,
    @SerialName("total_records")
    val totalRecords: Long,
    @SerialName("data")
    val participantData: List<ParticipantData>
)
