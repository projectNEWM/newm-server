package io.newm.server.features.user.database

import io.newm.server.auth.oauth.OAuthType
import io.newm.server.ext.existsHavingId
import io.newm.server.ext.getId
import io.newm.server.features.user.model.User
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.lowerCase
import java.util.UUID

class UserEntity(id: EntityID<UUID>) : UUIDEntity(id) {

    var oauthType by UserTable.oauthType
    var oauthId by UserTable.oauthId
    var firstName by UserTable.firstName
    var lastName by UserTable.lastName
    var nickname by UserTable.nickname
    var pictureUrl by UserTable.pictureUrl
    var role by UserTable.role
    var genre by UserTable.genre
    var email by UserTable.email
    var passwordHash by UserTable.passwordHash

    fun toModel(includeAll: Boolean = true) = User(
        id = id.value,
        oauthType = oauthType.takeIf { includeAll },
        oauthId = oauthId.takeIf { includeAll },
        firstName = firstName,
        lastName = lastName,
        nickname = nickname,
        pictureUrl = pictureUrl,
        role = role,
        genre = genre,
        email = email.takeIf { includeAll }
    )

    companion object : UUIDEntityClass<UserEntity>(UserTable) {

        fun getByEmail(email: String): UserEntity? = find {
            UserTable.email.lowerCase() eq email.lowercase()
        }.firstOrNull()

        fun existsByEmail(email: String): Boolean = existsHavingId {
            UserTable.email.lowerCase() eq email.lowercase()
        }

        fun getId(oauthType: OAuthType, oauthId: String): UUID? = UserTable.getId {
            (UserTable.oauthType eq oauthType) and (UserTable.oauthId eq oauthId)
        }
    }
}
