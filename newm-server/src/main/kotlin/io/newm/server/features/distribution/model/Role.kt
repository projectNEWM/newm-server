package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Role(
    @SerialName("role_id")
    val roleId: Long,
    @SerialName("name")
    val name: String
)
