package io.newm.server.features.walletconnection.model

import io.newm.shared.serialization.LocalDateTimeSerializer
import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class GenerateChallengeResponse(
    @Serializable(with = UUIDSerializer::class)
    val challengeId: UUID,
    @Serializable(with = LocalDateTimeSerializer::class)
    val expiresAt: LocalDateTime,
    // hex_string(data) or cbor(transaction) to be signed by requester (depending on GenerateChallengeRequest.method)
    val payload: String
)
