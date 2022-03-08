package io.projectnewm.server.user.database

import io.projectnewm.server.oauth.OAuthType
import org.jetbrains.exposed.dao.id.UUIDTable

object UserTable : UUIDTable(name = "users") {
    val oauthType = enumeration("oauth_type", OAuthType::class).nullable()
    val oauthId = text("oauth_id").nullable()
    val firstName = text("first_name").nullable()
    val lastName = text("last_name").nullable()
    val pictureUrl = text("picture_url").nullable()
    val email = text("email").nullable()
}
