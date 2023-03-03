package io.newm.server.features.user.database

import io.newm.server.auth.oauth.OAuthType
import io.newm.server.ext.existsHavingId
import io.newm.server.ext.getId
import io.newm.server.features.user.model.User
import io.newm.server.features.user.model.UserFilters
import io.newm.server.features.user.model.UserVerificationStatus
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import java.util.UUID

class UserEntity(id: EntityID<UUID>) : UUIDEntity(id) {

    var oauthType: OAuthType? by UserTable.oauthType
    var oauthId: String? by UserTable.oauthId
    var firstName: String? by UserTable.firstName
    var lastName: String? by UserTable.lastName
    var nickname: String? by UserTable.nickname
    var pictureUrl: String? by UserTable.pictureUrl
    var role: String? by UserTable.role
    var genre: String? by UserTable.genre
    var walletAddress: String? by UserTable.walletAddress
    var email: String by UserTable.email
    var passwordHash: String? by UserTable.passwordHash
    var verificationStatus: UserVerificationStatus by UserTable.verificationStatus

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
        walletAddress = walletAddress.takeIf { includeAll },
        email = email.takeIf { includeAll },
        verificationStatus = verificationStatus.takeIf { includeAll }
    )

    companion object : UUIDEntityClass<UserEntity>(UserTable) {
        fun all(filters: UserFilters): SizedIterable<UserEntity> {
            val ops = mutableListOf<Op<Boolean>>()
            with(filters) {
                ids?.let {
                    ops += UserTable.id inList it
                }
                roles?.let {
                    ops += UserTable.role inList it
                }
                genres?.let {
                    ops += UserTable.genre inList it
                }
            }
            return if (ops.isEmpty()) all() else find(AndOp(ops))
        }

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
