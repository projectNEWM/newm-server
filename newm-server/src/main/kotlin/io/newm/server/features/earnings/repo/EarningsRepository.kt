package io.newm.server.features.earnings.repo

import io.newm.server.features.earnings.model.ClaimOrder
import io.newm.server.features.earnings.model.Earning
import java.util.UUID

interface EarningsRepository {
    /**
     * Add a new earning
     */
    suspend fun add(earning: Earning): UUID

    /**
     * Add a new claim order
     */
    suspend fun add(claimOrder: ClaimOrder): UUID

    /**
     * Mark the earnings as claimed by a claim order
     */
    suspend fun claimed(
        earningIds: List<UUID>,
        claimOrderId: UUID
    )

    /**
     * Get all earnings by stake address
     */
    suspend fun getAllByStakeAddress(stakeAddress: String): List<Earning>
}
