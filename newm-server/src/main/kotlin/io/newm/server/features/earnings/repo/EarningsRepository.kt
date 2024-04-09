package io.newm.server.features.earnings.repo

import io.newm.server.features.earnings.model.AddSongRoyaltyRequest
import io.newm.server.features.earnings.model.ClaimOrder
import io.newm.server.features.earnings.model.Earning
import java.util.*

interface EarningsRepository {
    /**
     * Add a new earning
     */
    suspend fun add(earning: Earning): UUID

    /**
     * Add a batch of earnings
     */
    suspend fun addAll(earnings: List<Earning>)

    /**
     * Add royalty earnings to a song and split them based on its stream token holders
     */
    suspend fun addRoyaltySplits(
        songId: UUID,
        royaltyRequest: AddSongRoyaltyRequest
    )

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
     * Get all earnings
     */
    suspend fun getAll(): List<Earning>

    /**
     * Get all earnings by song id
     */
    suspend fun getAllBySongId(songId: UUID): List<Earning>

    /**
     * Get all earnings by stake address
     */
    suspend fun getAllByStakeAddress(stakeAddress: String): List<Earning>
}
