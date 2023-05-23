package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParticipantData(
    @SerialName("participant_id")
    val participantId: Long,
    @SerialName("ipn")
    val ipn: String,
    @SerialName("isni")
    val isni: String,
    @SerialName("is_active")
    val isActive: Int,
    @SerialName("removable")
    val removable: Boolean,
    @SerialName("name")
    val name: String
)
