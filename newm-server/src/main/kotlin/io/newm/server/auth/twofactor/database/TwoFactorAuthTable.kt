package io.newm.server.auth.twofactor.database

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime

object TwoFactorAuthTable : LongIdTable(name = "two_factor_auth") {
    val email: Column<String> = text("email")
    val codeHash: Column<String> = text("code_hash")
    val expiresAt: Column<LocalDateTime> = datetime("expires_at")
}
