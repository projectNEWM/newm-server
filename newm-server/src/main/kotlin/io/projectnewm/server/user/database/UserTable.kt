package io.projectnewm.server.user.database

import io.projectnewm.server.auth.oauth.OAuthType
import org.jetbrains.exposed.dao.id.UUIDTable

object UserTable : UUIDTable(name = "users") {
    val oauthType = enumeration("oauth_type", OAuthType::class).nullable()
    val oauthId = text("oauth_id").nullable()
    val firstName = text("first_name")
    val lastName = text("last_name")
    val pictureUrl = text("picture_url").nullable()
    val email = text("email")
    val passwordHash = text("password_hash").nullable()
}
