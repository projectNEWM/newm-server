package io.newm.server.features.collaboration.model

import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CollaborationIdBody(
    @Serializable(with = UUIDSerializer::class)
    val collaborationId: UUID
)
