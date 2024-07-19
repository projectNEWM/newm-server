package io.newm.server.features.earnings.repo

import io.newm.server.features.earnings.model.AddSongRoyaltyRequest
import io.newm.server.features.earnings.model.ClaimOrder
import io.newm.server.features.earnings.model.Earning
import io.newm.server.typealiases.SongId
import java.util.UUID

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
        songId: SongId,
        royaltyRequest: AddSongRoyaltyRequest
    )

    /**
     * Add a new claim order
     */
    suspend fun add(claimOrder: ClaimOrder): UUID

    /**
     * Create a new claim order for a stake address
     */
    suspend fun createClaimOrder(stakeAddress: String): ClaimOrder?

    /**
     * Update a claim order
     */
    suspend fun update(claimOrder: ClaimOrder)

    /**
     * Mark the earnings as claimed by a claim order
     */
    suspend fun claimed(
        earningsIds: List<UUID>,
        claimOrderId: UUID
    )

    /**
     * Get all earnings
     */
    suspend fun getAll(): List<Earning>

    /**
     * Get all earnings by song id
     */
    suspend fun getAllBySongId(songId: SongId): List<Earning>

    /**
     * Get all earnings by stake address
     */
    suspend fun getAllByStakeAddress(stakeAddress: String): List<Earning>

    /**
     * Get all earnings by stake address and claimed status
     */
    suspend fun getAllUnclaimedByStakeAddress(stakeAddress: String): List<Earning>

    /**
     * Get all earnings by ids
     */
    suspend fun getAllByIds(earningsIds: List<UUID>): List<Earning>

    /**
     * Get the active claim order by stake address. null if there is no active claim order
     */
    suspend fun getActiveClaimOrderByStakeAddress(stakeAddress: String): ClaimOrder?

    /**
     * Get a claim order by id
     */
    suspend fun getByClaimOrderId(claimOrderId: UUID): ClaimOrder?
}
