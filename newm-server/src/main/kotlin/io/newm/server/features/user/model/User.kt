package io.newm.server.features.user.model

import io.newm.server.auth.oauth.model.OAuthType
import io.newm.shared.auth.Password
import io.newm.shared.serialization.LocalDateTimeSerializer
import io.newm.shared.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class User(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
    val oauthType: OAuthType? = null,
    val oauthId: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val nickname: String? = null,
    val pictureUrl: String? = null,
    val bannerUrl: String? = null,
    val websiteUrl: String? = null,
    val twitterUrl: String? = null,
    val instagramUrl: String? = null,
    val location: String? = null,
    val role: String? = null,
    val genre: String? = null,
    val biography: String? = null,
    val walletAddress: String? = null,
    val email: String? = null,
    val newPassword: Password? = null,
    val confirmPassword: Password? = null,
    val currentPassword: Password? = null,
    val authCode: String? = null,
    val verificationStatus: UserVerificationStatus? = null,
    val companyName: String? = null,
    val companyLogoUrl: String? = null,
    var companyIpRights: Boolean? = null,
    @Serializable(with = UUIDSerializer::class)
    @Transient
    var distributionUserId: UUID? = null,
    @Transient
    var distributionArtistId: Long? = null,
    @Transient
    var distributionParticipantId: Long? = null,
    @Transient
    var distributionSubscriptionId: Long? = null,
    @Transient
    var distributionLabelId: Long? = null,
    var distributionIsni: String? = null,
    var distributionIpn: String? = null,
) {
    val stageOrFullName: String by lazy { nickname ?: "$firstName $lastName".trim() }
}
