package io.newm.server.features.walletconnection.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class AnswerChallengeResponse(
    @Contextual
    val connectionId: UUID,
    @Contextual
    val expiresAt: LocalDateTime
)
