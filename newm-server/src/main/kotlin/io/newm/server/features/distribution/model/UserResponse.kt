package io.newm.server.features.distribution.model

import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class UserResponse(
    @SerialName("uuid")
    @Serializable(with = UUIDSerializer::class)
    val uuid: UUID,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("sur_name")
    val lastName: String,
    @SerialName("email")
    val email: String,
)