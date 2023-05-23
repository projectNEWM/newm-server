package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Availability(
    @SerialName("availability_id")
    val availabilityId: Long,
    @SerialName("name")
    val name: String
)
