package io.newm.server.features.user.model

import com.google.common.annotations.VisibleForTesting
import io.newm.server.auth.oauth.model.OAuthType
import io.newm.server.model.ClientPlatform
import io.newm.server.typealiases.UserId
import io.newm.shared.auth.Password
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.LocalDateTime

@Serializable
data class User(
    @Contextual
    val id: UserId? = null,
    @Contextual
    val createdAt: LocalDateTime? = null,
    val oauthType: OAuthType? = null,
    val signupPlatform: ClientPlatform? = null,
    val oauthId: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    // nickname should be accessed through stageOrFullName
    @VisibleForTesting
    val nickname: String? = null,
    val pictureUrl: String? = null,
    val bannerUrl: String? = null,
    val websiteUrl: String? = null,
    val twitterUrl: String? = null,
    val instagramUrl: String? = null,
    val spotifyProfile: String? = null,
    val soundCloudProfile: String? = null,
    val appleMusicProfile: String? = null,
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
    val companyIpRights: Boolean? = null,
    val isni: String? = null,
    val ipi: String? = null,
    val dspPlanSubscribed: Boolean? = null,
    @Transient
    var distributionUserId: String? = null,
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
    @Transient
    var distributionNewmParticipantId: Long? = null,
) {
    val stageOrFullName: String by lazy { stageOrFullNameOf(nickname, firstName, lastName) }

    val companyOrStageOrFullName: String by lazy {
        if (companyIpRights == true && !companyName.isNullOrBlank()) {
            companyName
        } else {
            stageOrFullNameOf(
                nickname,
                firstName,
                lastName
            )
        }
    }

    companion object {
        fun stageOrFullNameOf(
            nickname: String?,
            firstName: String?,
            lastName: String?
        ): String = if (nickname.isNullOrBlank()) "${firstName.orEmpty().trim()} ${lastName.orEmpty().trim()}".trim() else nickname
    }
}
