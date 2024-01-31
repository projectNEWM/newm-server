package io.newm.server.features.earnings.database

import io.newm.server.features.cardano.database.KeyTable
import io.newm.shared.exposed.uuidArray
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object ClaimOrdersTable : UUIDTable(name = "claim_orders") {
    // The user's stake address
    val stakeAddress: Column<String> = text("stake_address")

    // The key/address used to receive payment for the claiming transaction
    val keyId: Column<EntityID<UUID>> = reference("key_id", KeyTable, onDelete = ReferenceOption.RESTRICT)

    // The status of the claim order
    val status: Column<String> = text("status")

    // The IDs of the earnings that are being claimed
    val earningsIds: Column<Array<UUID>> = uuidArray("earnings_ids")

    // The IDs of the earnings that failed to be claimed (bucket ran dry or some other error)
    val failedEarningsIds: Column<Array<UUID>?> = uuidArray("failed_earnings_ids").nullable()

    // The transaction ID of the claiming transaction
    val transactionId: Column<String?> = text("transaction_id").nullable()

    // The time the claim order was created
    val createdAt: Column<LocalDateTime> = datetime("created_at").defaultExpression(CurrentDateTime)
}
