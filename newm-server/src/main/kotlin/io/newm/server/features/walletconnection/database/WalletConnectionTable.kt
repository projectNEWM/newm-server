package io.newm.server.features.walletconnection.database

import io.newm.server.features.user.database.UserTable
import io.newm.server.features.walletconnection.model.WalletChain
import io.newm.server.typealiases.UserId
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object WalletConnectionTable : UUIDTable(name = "wallet_connections") {
    val createdAt: Column<LocalDateTime> = datetime("created_at").defaultExpression(CurrentDateTime)
    val chain: Column<WalletChain> = enumeration("chain", WalletChain::class)
    val address: Column<String> = text("address")
    val name: Column<String> = text("name")
    val userId: Column<EntityID<UserId>?> = reference("user_id", UserTable, onDelete = ReferenceOption.NO_ACTION).nullable()
}
