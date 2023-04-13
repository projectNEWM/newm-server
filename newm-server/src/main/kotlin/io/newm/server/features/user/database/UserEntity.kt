package io.newm.server.features.user.database

import io.newm.server.auth.oauth.OAuthType
import io.newm.server.features.user.model.User
import io.newm.server.features.user.model.UserFilters
import io.newm.server.features.user.model.UserVerificationStatus
import io.newm.shared.ktx.exists
import io.newm.shared.ktx.getId
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.AndOp
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.lowerCase
import java.time.LocalDateTime
import java.util.UUID

class UserEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    val createdAt: LocalDateTime by UserTable.createdAt
    var oauthType: OAuthType? by UserTable.oauthType
    var oauthId: String? by UserTable.oauthId
    var firstName: String? by UserTable.firstName
    var lastName: String? by UserTable.lastName
    var nickname: String? by UserTable.nickname
    var pictureUrl: String? by UserTable.pictureUrl
    var bannerUrl: String? by UserTable.bannerUrl
    var websiteUrl: String? by UserTable.websiteUrl
    var twitterUrl: String? by UserTable.twitterUrl
    var instagramUrl: String? by UserTable.instagramUrl
    var location: String? by UserTable.location
    var role: String? by UserTable.role
    var genre: String? by UserTable.genre
    var biography: String? by UserTable.biography
    var walletAddress: String? by UserTable.walletAddress
    var email: String by UserTable.email
    var passwordHash: String? by UserTable.passwordHash
    var verificationStatus: UserVerificationStatus by UserTable.verificationStatus
    var companyName: String? by UserTable.companyName
    var companyLogoUrl: String? by UserTable.companyLogoUrl
    var companyIpRights: Boolean? by UserTable.companyIpRights

    fun toModel(includeAll: Boolean = true) = User(
        id = id.value,
        createdAt = createdAt,
        oauthType = oauthType.takeIf { includeAll },
        oauthId = oauthId.takeIf { includeAll },
        firstName = firstName,
        lastName = lastName,
        nickname = nickname,
        pictureUrl = pictureUrl,
        bannerUrl = bannerUrl,
        websiteUrl = websiteUrl,
        twitterUrl = twitterUrl,
        instagramUrl = instagramUrl,
        location = location,
        role = role,
        genre = genre,
        biography = biography,
        walletAddress = walletAddress.takeIf { includeAll },
        email = email.takeIf { includeAll },
        verificationStatus = verificationStatus.takeIf { includeAll },
        companyName = companyName,
        companyLogoUrl = companyLogoUrl,
        companyIpRights = companyIpRights
    )

    companion object : UUIDEntityClass<UserEntity>(UserTable) {
        fun all(filters: UserFilters): SizedIterable<UserEntity> {
            val ops = mutableListOf<Op<Boolean>>()
            with(filters) {
                olderThan?.let {
                    ops += UserTable.createdAt less it
                }
                newerThan?.let {
                    ops += UserTable.createdAt greater it
                }
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

        fun existsByEmail(email: String): Boolean = exists {
            UserTable.email.lowerCase() eq email.lowercase()
        }

        fun getId(oauthType: OAuthType, oauthId: String): UUID? = UserTable.getId {
            (UserTable.oauthType eq oauthType) and (UserTable.oauthId eq oauthId)
        }
    }
}
