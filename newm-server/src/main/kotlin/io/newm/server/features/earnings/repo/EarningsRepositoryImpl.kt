package io.newm.server.features.earnings.repo

import io.newm.server.features.cardano.database.KeyTable
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.earnings.database.ClaimOrderEntity
import io.newm.server.features.earnings.database.ClaimOrdersTable
import io.newm.server.features.earnings.database.EarningEntity
import io.newm.server.features.earnings.database.EarningsTable
import io.newm.server.features.earnings.model.AddSongRoyaltyRequest
import io.newm.server.features.earnings.model.ClaimOrder
import io.newm.server.features.earnings.model.Earning
import io.newm.server.features.song.database.SongTable
import io.newm.server.features.song.repo.SongRepository
import io.newm.server.features.user.repo.UserRepository
import io.newm.server.typealiases.SongId
import java.time.LocalDateTime
import java.util.UUID
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.transaction

class EarningsRepositoryImpl(
    private val userRepository: UserRepository,
    private val songRepository: SongRepository,
    private val cardanoRepository: CardanoRepository,
) : EarningsRepository {
    override suspend fun add(earning: Earning): UUID =
        transaction {
            EarningEntity.new {
                this.songId = earning.songId?.let { EntityID(it, SongTable) }
                this.stakeAddress = earning.stakeAddress
                this.amount = earning.amount
                this.memo = earning.memo
                this.startDate = earning.startDate
                this.endDate = earning.endDate
                this.claimed = earning.claimed
                this.claimedAt = earning.claimedAt
                this.claimOrderId = earning.claimOrderId?.let { EntityID(it, ClaimOrdersTable) }
                this.createdAt = earning.createdAt
            }.id.value
        }

    override suspend fun addAll(earnings: List<Earning>) {
        transaction {
            EarningsTable.batchInsert(earnings) { earning ->
                this[EarningsTable.songId] = earning.songId?.let { EntityID(it, SongTable) }
                this[EarningsTable.stakeAddress] = earning.stakeAddress
                this[EarningsTable.amount] = earning.amount
                this[EarningsTable.memo] = earning.memo
                this[EarningsTable.startDate] = earning.startDate
                this[EarningsTable.endDate] = earning.endDate
                this[EarningsTable.claimed] = earning.claimed
                this[EarningsTable.claimedAt] = earning.claimedAt
                this[EarningsTable.claimedOrderId] = earning.claimOrderId?.let { EntityID(it, ClaimOrdersTable) }
                this[EarningsTable.createdAt] = earning.createdAt
            }
        }
    }

    override suspend fun addRoyaltySplits(
        songId: SongId,
        royaltyRequest: AddSongRoyaltyRequest
    ) {
        require((royaltyRequest.newmAmount != null) xor (royaltyRequest.usdAmount != null)) {
            "Either newmAmount or usdAmount must be provided, but not both."
        }

        val song = songRepository.get(songId)
        val user = userRepository.get(song.ownerId!!)

        val snapshotResponse = cardanoRepository.snapshotToken(policyId = song.nftPolicyId!!, name = song.nftName!!)
        require(snapshotResponse.snapshotEntriesCount > 0) {
            "No snapshot entries found for song."
        }

        val snapshotMap = snapshotResponse.snapshotEntriesList.associate { it.stakeAddress to it.amount.toBigDecimal() }

        // should be 100m stream tokens
        val totalSupply = snapshotMap["total_supply"] ?: error("No total supply found in snapshot.")
        require(totalSupply == 100_000_000.toBigDecimal()) {
            "Total supply of stream tokens must be 100m."
        }

        val now = LocalDateTime.now()
        var exchangeRate = ""
        val totalNewmAmount =
            royaltyRequest.newmAmount?.toBigDecimal() ?: run {
                val usdAmount = royaltyRequest.usdAmount!!
                val newmUsdPrice = cardanoRepository.queryNEWMUSDPrice()
                val newmAmount = (newmUsdPrice.toBigInteger() * usdAmount).toBigDecimal()
                exchangeRate = " @ 1 NEWM = ${newmUsdPrice.toBigDecimal().movePointLeft(6).toPlainString()} USD"
                newmAmount
            }

        val earnings =
            snapshotMap.mapNotNull { (stakeAddress, streamTokenAmount) ->
                if (stakeAddress == "total_supply") return@mapNotNull null

                // calculate the amount of royalties for this stake address
                // wait 24 hours before starting the royalties in case somebody
                // put in a number wrong.
                Earning(
                    songId = songId,
                    stakeAddress = stakeAddress,
                    amount = (totalNewmAmount * (streamTokenAmount / totalSupply)).toLong(),
                    memo = "Royalty for: ${song.title} - ${user.stageOrFullName}$exchangeRate",
                    createdAt = now,
                    startDate = now.plusHours(24),
                )
            }
        addAll(earnings)
    }

    override suspend fun add(claimOrder: ClaimOrder): UUID =
        transaction {
            ClaimOrderEntity.new {
                this.stakeAddress = claimOrder.stakeAddress
                this.keyId = EntityID(claimOrder.keyId, KeyTable)
                this.status = claimOrder.status.name
                this.earningsIds = claimOrder.earningsIds.toTypedArray()
                this.failedEarningsIds = claimOrder.failedEarningsIds?.toTypedArray()
                this.transactionId = claimOrder.transactionId
                this.createdAt = claimOrder.createdAt
            }.id.value
        }

    override suspend fun claimed(
        earningIds: List<UUID>,
        claimOrderId: UUID
    ) {
        transaction {
            val claimedAt = LocalDateTime.now()
            EarningEntity.forIds(earningIds).forUpdate().forEach {
                it.claimed = true
                it.claimOrderId = EntityID(claimOrderId, ClaimOrdersTable)
                it.claimedAt = claimedAt
            }
        }
    }

    override suspend fun getAll(): List<Earning> =
        transaction {
            EarningEntity.all().map { it.toModel() }
        }

    override suspend fun getAllBySongId(songId: SongId): List<Earning> =
        transaction {
            EarningEntity.wrapRows(
                EarningEntity.searchQuery(EarningsTable.songId eq songId).orderBy(EarningsTable.createdAt, SortOrder.DESC)
            ).map { it.toModel() }
        }

    override suspend fun getAllByStakeAddress(stakeAddress: String): List<Earning> =
        transaction {
            EarningEntity.wrapRows(
                EarningEntity.searchQuery(EarningsTable.stakeAddress eq stakeAddress).orderBy(EarningsTable.createdAt, SortOrder.DESC)
            ).map { it.toModel() }
        }
}
