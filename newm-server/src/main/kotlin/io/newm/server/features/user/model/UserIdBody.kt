package io.newm.server.features.user.model

import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class UserIdBody(
    @Serializable(with = UUIDSerializer::class)
    val userId: UUID
)
