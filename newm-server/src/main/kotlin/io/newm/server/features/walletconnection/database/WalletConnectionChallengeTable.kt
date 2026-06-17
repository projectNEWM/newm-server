package io.newm.server.features.walletconnection.database

import io.newm.server.features.walletconnection.model.ChallengeMethod
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime

object WalletConnectionChallengeTable : UUIDTable(name = "wallet_connection_challenges") {
    val createdAt: Column<LocalDateTime> = datetime("created_at").defaultExpression(CurrentDateTime)
    val method: Column<ChallengeMethod> = enumeration("method")
    val stakeAddress: Column<String> = text("stake_address")
}
