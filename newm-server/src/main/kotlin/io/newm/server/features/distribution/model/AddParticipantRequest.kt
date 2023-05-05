package io.newm.server.features.distribution.model

import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class AddParticipantRequest(
    @SerialName("uuid")
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    @SerialName("name")
    val name: String,
    @SerialName("isni")
    val isni: String?,
    @SerialName("ipn")
    val ipn: String?,
)
