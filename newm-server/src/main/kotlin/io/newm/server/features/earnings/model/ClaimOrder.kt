package io.newm.server.features.earnings.model

import io.newm.shared.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class ClaimOrder(
    @Contextual
    val id: UUID,
    val stakeAddress: String,
    @Contextual
    val keyId: UUID,
    val status: ClaimOrderStatus,
    val earningsIds: List<
        @Contextual
        UUID
    >,
    val failedEarningsIds: List<
        @Contextual UUID
    >?,
    val transactionId: String?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime
)
