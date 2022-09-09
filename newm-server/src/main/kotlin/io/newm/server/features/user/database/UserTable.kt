package io.newm.server.features.user.database

import io.newm.server.auth.oauth.OAuthType
import org.jetbrains.exposed.dao.id.UUIDTable

object UserTable : UUIDTable(name = "users") {
    val oauthType = enumeration("oauth_type", OAuthType::class).nullable()
    val oauthId = text("oauth_id").nullable()
    val firstName = text("first_name").nullable()
    val lastName = text("last_name").nullable()
    val nickname = text("nickname").nullable()
    val pictureUrl = text("picture_url").nullable()
    val role = text("role").nullable()
    val genre = text("genre").nullable()
    val walletAddress = text("wallet_address").nullable()
    val email = text("email")
    val passwordHash = text("password_hash").nullable()
}
