package io.projectnewm.server.portal.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Contributor(
    @SerialName("name") val name: String,
    @SerialName("role") val role: Role,
    @SerialName("stake") val stake: Double
)
