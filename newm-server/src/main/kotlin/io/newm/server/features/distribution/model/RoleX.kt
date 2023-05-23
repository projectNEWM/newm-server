package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoleX(
    @SerialName("role_id")
    val roleId: String,
    @SerialName("role_name")
    val roleName: String
)
