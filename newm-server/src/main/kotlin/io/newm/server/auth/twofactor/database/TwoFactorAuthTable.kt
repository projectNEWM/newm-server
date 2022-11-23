package io.newm.server.auth.twofactor.database

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object TwoFactorAuthTable : LongIdTable(name = "two_factor_auth") {
    val email: Column<String> = text("email")
    val codeHash: Column<String> = text("code_hash")
    val expiresAt: Column<LocalDateTime> = datetime("expires_at")
}
