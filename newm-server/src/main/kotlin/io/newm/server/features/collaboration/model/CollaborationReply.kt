package io.newm.server.features.collaboration.model

import kotlinx.serialization.Serializable

@Serializable
data class CollaborationReply(
    val accepted: Boolean
)
