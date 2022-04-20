package io.projectnewm.server.features.user.database

import io.projectnewm.server.auth.oauth.OAuthType
import io.projectnewm.server.database.textArray
import org.jetbrains.exposed.dao.id.UUIDTable

object UserTable : UUIDTable(name = "users") {
    val oauthType = enumeration("oauth_type", OAuthType::class).nullable()
    val oauthId = text("oauth_id").nullable()
    val firstName = text("first_name").nullable()
    val lastName = text("last_name").nullable()
    val nickname = text("nickname").nullable()
    val pictureUrl = text("picture_url").nullable()
    val role = text("role").nullable()
    val genres = textArray("genres").nullable()
    val email = text("email")
    val passwordHash = text("password_hash").nullable()
}
