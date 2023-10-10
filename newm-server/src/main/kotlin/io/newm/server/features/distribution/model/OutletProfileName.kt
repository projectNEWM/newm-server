package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OutletProfileName(
    @SerialName("id")
    val id: Long,
    @SerialName("name")
    val name: String,
)
