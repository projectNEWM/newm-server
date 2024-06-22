package io.newm.server.features.user.database

import io.newm.server.auth.oauth.model.OAuthType
import io.newm.server.features.user.model.User
import io.newm.server.features.user.model.UserFilters
import io.newm.server.features.user.model.UserVerificationStatus
import io.newm.server.typealiases.UserId
import io.newm.shared.ktx.exists
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.AndOp
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.notInList
import org.jetbrains.exposed.sql.lowerCase
import java.time.LocalDateTime

open class UserEntity(
    id: EntityID<UserId>
) : UUIDEntity(id) {
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
    var spotifyProfile: String? by UserTable.spotifyProfile
    var soundCloudProfile: String? by UserTable.soundCloudProfile
    var appleMusicProfile: String? by UserTable.appleMusicProfile
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
    var isni: String? by UserTable.isni
    var ipi: String? by UserTable.ipi
    var dspPlanSubscribed: Boolean by UserTable.dspPlanSubscribed
    var admin: Boolean by UserTable.admin
    var distributionUserId: String? by UserTable.distributionUserId
    var distributionArtistId: Long? by UserTable.distributionArtistId
    var distributionParticipantId: Long? by UserTable.distributionParticipantId
    var distributionSubscriptionId: Long? by UserTable.distributionSubscriptionId
    var distributionLabelId: Long? by UserTable.distributionLabelId
    var distributionIsni: String? by UserTable.distributionIsni
    var distributionIpn: String? by UserTable.distributionIpn
    var distributionNewmParticipantId: Long? by UserTable.distributionNewmParticipantId

    fun toModel(includeAll: Boolean = true) =
        User(
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
            spotifyProfile = spotifyProfile,
            soundCloudProfile = soundCloudProfile,
            appleMusicProfile = appleMusicProfile,
            location = location,
            role = role,
            genre = genre,
            biography = biography,
            walletAddress = walletAddress.takeIf { includeAll },
            email = email.takeIf { includeAll },
            verificationStatus = verificationStatus.takeIf { includeAll },
            companyName = companyName,
            companyLogoUrl = companyLogoUrl,
            companyIpRights = companyIpRights,
            isni = isni,
            ipi = ipi,
            dspPlanSubscribed = dspPlanSubscribed,
            distributionUserId = distributionUserId,
            distributionArtistId = distributionArtistId,
            distributionParticipantId = distributionParticipantId,
            distributionSubscriptionId = distributionSubscriptionId,
            distributionLabelId = distributionLabelId,
            distributionIsni = distributionIsni,
            distributionIpn = distributionIpn,
            distributionNewmParticipantId = distributionNewmParticipantId
        )

    val stageOrFullName: String
        get() = User.stageOrFullNameOf(nickname, firstName, lastName)

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
                ids?.includes?.let {
                    ops += UserTable.id inList it
                }
                ids?.excludes?.let {
                    ops += UserTable.id notInList it
                }
                roles?.includes?.let {
                    ops += UserTable.role inList it
                }
                roles?.excludes?.let {
                    ops += UserTable.role notInList it
                }
                genres?.includes?.let {
                    ops += UserTable.genre inList it
                }
                genres?.excludes?.let {
                    ops += UserTable.genre notInList it
                }
            }
            return (if (ops.isEmpty()) all() else find(AndOp(ops)))
                .orderBy(UserTable.createdAt to (filters.sortOrder ?: SortOrder.ASC))
        }

        fun getByEmail(email: String): UserEntity? =
            find {
                UserTable.email.lowerCase() eq email.lowercase()
            }.firstOrNull()

        fun existsByEmail(email: String): Boolean =
            exists {
                UserTable.email.lowerCase() eq email.lowercase()
            }
    }
}
