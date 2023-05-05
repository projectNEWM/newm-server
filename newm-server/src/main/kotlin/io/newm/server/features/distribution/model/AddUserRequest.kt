package io.newm.server.features.distribution.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddUserRequest(
    @SerialName("first_name")
    val firstName: String,
    @SerialName("sur_name")
    val lastName: String,
    @SerialName("email")
    val email: String,
)
