package io.newm.server.features.walletconnection.model

import io.newm.shared.serialization.LocalDateTimeSerializer
import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class AnswerChallengeResponse(
    @Serializable(with = UUIDSerializer::class)
    val connectionId: UUID,
    @Serializable(with = LocalDateTimeSerializer::class)
    val expiresAt: LocalDateTime
)
