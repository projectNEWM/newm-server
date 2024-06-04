package io.newm.server.features.earnings.model

import io.newm.shared.serialization.LocalDateTimeSerializer
import io.newm.shared.serialization.UUIDSerializer
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class ClaimOrder(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val stakeAddress: String,
    @Serializable(with = UUIDSerializer::class)
    val keyId: UUID,
    val paymentAddress: String,
    val paymentAmount: Long,
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
    val createdAt: LocalDateTime,
    val errorMessage: String?
) {
    companion object {
        val ACTIVE_STATUSES = listOf(ClaimOrderStatus.Pending, ClaimOrderStatus.Processing).map { it.name }
    }
}
