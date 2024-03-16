package io.newm.server.features.walletconnection.database

import io.newm.server.features.user.database.UserTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object WalletConnectionTable : UUIDTable(name = "wallet_connections") {
    val createdAt: Column<LocalDateTime> = datetime("created_at").defaultExpression(CurrentDateTime)
    val stakeAddress: Column<String> = text("stake_address")
    val userId: Column<EntityID<UUID>?> = reference("user_id", UserTable, onDelete = ReferenceOption.NO_ACTION).nullable()
}
