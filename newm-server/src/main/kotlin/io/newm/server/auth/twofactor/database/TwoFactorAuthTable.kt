package io.newm.server.auth.twofactor.database

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object TwoFactorAuthTable : LongIdTable(name = "two_factor_auth") {
    val email = text("email")
    val codeHash = text("code_hash")
    val expiresAt = datetime("expires_at")
}
