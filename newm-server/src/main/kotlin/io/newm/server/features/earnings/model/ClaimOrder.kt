package io.newm.server.features.earnings.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class ClaimOrder(
    @Contextual
    val id: UUID? = null,
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
    @Contextual
    val createdAt: LocalDateTime,
    val errorMessage: String?,
    val cborHex: String,
) {
    companion object {
        val ACTIVE_STATUSES = listOf(ClaimOrderStatus.Pending, ClaimOrderStatus.Processing).map { it.name }
    }
}
