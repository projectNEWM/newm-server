package io.newm.server.features.earnings.repo

import io.newm.server.features.cardano.database.KeyTable
import io.newm.server.features.earnings.database.ClaimOrderEntity
import io.newm.server.features.earnings.database.ClaimOrdersTable
import io.newm.server.features.earnings.database.EarningEntity
import io.newm.server.features.earnings.model.ClaimOrder
import io.newm.server.features.earnings.model.Earning
import io.newm.server.features.song.database.SongTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

class EarningsRepositoryImpl : EarningsRepository {
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

    override suspend fun getAllByStakeAddress(stakeAddress: String): List<Earning> {
        TODO("Not yet implemented")
    }
}
