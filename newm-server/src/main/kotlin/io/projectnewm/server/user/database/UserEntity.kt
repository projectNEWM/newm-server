package io.projectnewm.server.user.database

import io.projectnewm.server.oauth.OAuthType
import io.projectnewm.server.user.User
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and
import java.util.UUID

class UserEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UserEntity>(UserTable)

    var oauthType by UserTable.oauthType
    var oauthId by UserTable.oauthId
    var firstName by UserTable.firstName
    var lastName by UserTable.lastName
    var pictureUrl by UserTable.pictureUrl
    var email by UserTable.email

    fun toModel(includeAll: Boolean = true) = User(
        id = id.value,
        oauthType = oauthType.takeIf { includeAll },
        oauthId = oauthId.takeIf { includeAll },
        firstName = firstName,
        lastName = lastName,
        pictureUrl = pictureUrl,
        email = email.takeIf { includeAll }
    )
}

fun UUIDEntityClass<UserEntity>.find(oauthType: OAuthType, oauthId: String): UserEntity? = find {
    UserTable.oauthType.eq(oauthType) and UserTable.oauthId.eq(oauthId)
}.firstOrNull()
