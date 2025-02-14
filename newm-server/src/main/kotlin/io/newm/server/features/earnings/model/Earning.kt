package io.newm.server.features.earnings.model

import io.newm.server.typealiases.SongId
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.UUID

/**
 * An earning is any type of reward earned by the user. It could be a music royalty from stream tokens. It could also be
 * a reward they earned from the learn-to-earn program or any general marketing team reward we do.
 */
@Serializable
data class Earning(
    @Contextual
    val id: UUID? = null,
    @Contextual
    val songId: SongId? = null,
    val stakeAddress: String,
    val amount: Long,
    val memo: String,
    @Contextual
    val startDate: LocalDateTime? = null,
    @Contextual
    val endDate: LocalDateTime? = null,
    val claimed: Boolean = false,
    @Contextual
    val claimedAt: LocalDateTime? = null,
    @Contextual
    val claimOrderId: UUID? = null,
    @Contextual
    val createdAt: LocalDateTime,
    val nftPolicyId: String? = null,
    val nftAssetName: String? = null
) {
    val isActive: Boolean
        get() {
            val now = LocalDateTime.now()
            return (startDate == null || startDate < now) &&
                (endDate == null || endDate > now)
        }
}
