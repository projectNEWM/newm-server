package io.newm.server.features.collaboration.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CollaborationIdBody(
    @Contextual
    val collaborationId: UUID
)
