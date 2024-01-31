package io.newm.server.features.earnings.database

import io.newm.server.features.earnings.model.ClaimOrder
import io.newm.server.features.earnings.model.ClaimOrderStatus
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.UUID

class ClaimOrderEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    var stakeAddress by ClaimOrdersTable.stakeAddress
    var keyId by ClaimOrdersTable.keyId
    var status by ClaimOrdersTable.status
    var earningsIds by ClaimOrdersTable.earningsIds
    var failedEarningsIds by ClaimOrdersTable.failedEarningsIds
    var transactionId by ClaimOrdersTable.transactionId
    var createdAt by ClaimOrdersTable.createdAt

    fun toModel() =
        ClaimOrder(
            id = id.value,
            stakeAddress = stakeAddress,
            keyId = keyId.value,
            status = ClaimOrderStatus.valueOf(status),
            earningsIds = earningsIds.toList(),
            failedEarningsIds = failedEarningsIds?.toList(),
            transactionId = transactionId,
            createdAt = createdAt
        )

    companion object : UUIDEntityClass<ClaimOrderEntity>(ClaimOrdersTable)
}
