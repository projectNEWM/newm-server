package io.newm.server.features.earnings.model

import io.newm.shared.serialization.LocalDateTimeSerializer
import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class ClaimOrder(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val stakeAddress: String,
    @Serializable(with = UUIDSerializer::class)
    val keyId: UUID,
    val status: ClaimOrderStatus,
    val earningsIds: List<
        @Serializable(with = UUIDSerializer::class)
        UUID
    >,
    val failedEarningsIds: List<
        @Serializable(with = UUIDSerializer::class)
        UUID
    >?,
    val transactionId: String?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime
)
