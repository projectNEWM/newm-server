package io.projectnewm.server.auth.twofactor.database

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object TwoFactorAuthTable : IntIdTable(name = "two_factor_auth") {
    val email = text("email")
    val codeHash = text("code_hash")
    val expiresAt = datetime("expires_at")
}
