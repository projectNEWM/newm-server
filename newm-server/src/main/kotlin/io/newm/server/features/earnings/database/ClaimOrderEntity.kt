package io.newm.server.features.earnings.database

import io.newm.server.features.earnings.model.ClaimOrder
import io.newm.server.features.earnings.model.ClaimOrderStatus
import java.util.UUID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class ClaimOrderEntity(
    id: EntityID<UUID>
) : UUIDEntity(id) {
    var stakeAddress by ClaimOrdersTable.stakeAddress
    var keyId by ClaimOrdersTable.keyId
    var paymentAddress by ClaimOrdersTable.paymentAddress
    var paymentAmount by ClaimOrdersTable.paymentAmount
    var status by ClaimOrdersTable.status
    var earningsIds by ClaimOrdersTable.earningsIds
    var failedEarningsIds by ClaimOrdersTable.failedEarningsIds
    var transactionId by ClaimOrdersTable.transactionId
    var createdAt by ClaimOrdersTable.createdAt
    var errorMessage by ClaimOrdersTable.errorMessage

    fun toModel() =
        ClaimOrder(
            id = id.value,
            stakeAddress = stakeAddress,
            keyId = keyId.value,
            paymentAddress = paymentAddress,
            paymentAmount = paymentAmount,
            status = ClaimOrderStatus.valueOf(status),
            earningsIds = earningsIds.toList(),
            failedEarningsIds = failedEarningsIds?.toList(),
            transactionId = transactionId,
            createdAt = createdAt,
            errorMessage = errorMessage,
            cborHex = "",
        )

    companion object : UUIDEntityClass<ClaimOrderEntity>(ClaimOrdersTable)
}
