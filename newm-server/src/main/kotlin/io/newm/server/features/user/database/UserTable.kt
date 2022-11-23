package io.newm.server.features.user.database

import io.newm.server.auth.oauth.OAuthType
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column

object UserTable : UUIDTable(name = "users") {
    val oauthType: Column<OAuthType?> = enumeration("oauth_type", OAuthType::class).nullable()
    val oauthId: Column<String?> = text("oauth_id").nullable()
    val firstName: Column<String?> = text("first_name").nullable()
    val lastName: Column<String?> = text("last_name").nullable()
    val nickname: Column<String?> = text("nickname").nullable()
    val pictureUrl: Column<String?> = text("picture_url").nullable()
    val role: Column<String?> = text("role").nullable()
    val genre: Column<String?> = text("genre").nullable()
    val walletAddress: Column<String?> = text("wallet_address").nullable()
    val email: Column<String> = text("email")
    val passwordHash: Column<String?> = text("password_hash").nullable()
}
