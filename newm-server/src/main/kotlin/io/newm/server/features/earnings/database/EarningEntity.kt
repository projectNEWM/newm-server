package io.newm.server.features.earnings.database

import io.newm.server.features.earnings.model.Earning
import io.newm.server.typealiases.SongId
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.time.LocalDateTime
import java.util.UUID

class EarningEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    var songId: EntityID<SongId>? by EarningsTable.songId
    var stakeAddress: String by EarningsTable.stakeAddress
    var amount: Long by EarningsTable.amount
    var memo: String by EarningsTable.memo
    var startDate: LocalDateTime? by EarningsTable.startDate
    var endDate: LocalDateTime? by EarningsTable.endDate
    var claimed: Boolean by EarningsTable.claimed
    var claimedAt: LocalDateTime? by EarningsTable.claimedAt
    var claimOrderId: EntityID<UUID>? by EarningsTable.claimedOrderId
    var createdAt: LocalDateTime by EarningsTable.createdAt

    fun toModel(): Earning =
        Earning(
            id = id.value,
            songId = songId?.value,
            stakeAddress = stakeAddress,
            amount = amount,
            memo = memo,
            startDate = startDate,
            endDate = endDate,
            claimed = claimed,
            claimedAt = claimedAt,
            claimOrderId = claimOrderId?.value,
            createdAt = createdAt,
        )

    companion object : UUIDEntityClass<EarningEntity>(EarningsTable) {
    }
}
