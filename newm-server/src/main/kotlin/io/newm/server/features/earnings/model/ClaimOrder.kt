package io.newm.server.features.earnings.model

import io.newm.shared.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Contextual
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class ClaimOrder(
    @Contextual
    val id: UUID,
    val stakeAddress: String,
    @Contextual
    val keyId: UUID,
    val paymentAddress: String,
    val paymentAmount: Long,
    val status: ClaimOrderStatus,
    val earningsIds: List<
        @Contextual
        UUID
    >,
    val failedEarningsIds: List<
        @Contextual
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
