package io.newm.server.features.user.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class UserIdBody(
    @Contextual
    val userId: UUID
)
