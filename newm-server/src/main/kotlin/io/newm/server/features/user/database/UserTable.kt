package io.newm.server.features.user.database

import io.newm.server.auth.oauth.model.OAuthType
import io.newm.server.features.referralhero.model.ReferralStatus
import io.newm.server.features.user.model.UserVerificationStatus
import io.newm.server.model.ClientPlatform
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object UserTable : UUIDTable(name = "users") {
    val createdAt: Column<LocalDateTime> = datetime("created_at").defaultExpression(CurrentDateTime)
    val oauthType: Column<OAuthType?> = enumeration("oauth_type", OAuthType::class).nullable()
    val signupPlatform: Column<ClientPlatform> =
        enumeration("signup_platform", ClientPlatform::class).default(ClientPlatform.Studio)
    val oauthId: Column<String?> = text("oauth_id").nullable()
    val firstName: Column<String?> = text("first_name").nullable()
    val lastName: Column<String?> = text("last_name").nullable()
    val nickname: Column<String?> = text("nickname").nullable()
    val pictureUrl: Column<String?> = text("picture_url").nullable()
    val bannerUrl: Column<String?> = text("banner_url").nullable()
    val websiteUrl: Column<String?> = text("website_url").nullable()
    val twitterUrl: Column<String?> = text("twitter_url").nullable()
    val instagramUrl: Column<String?> = text("instagram_url").nullable()
    val spotifyProfile: Column<String?> = text("spotify_profile").nullable()
    val soundCloudProfile: Column<String?> = text("sound_cloud_profile").nullable()
    val appleMusicProfile: Column<String?> = text("apple_music_profile").nullable()
    val location: Column<String?> = text("location").nullable()
    val role: Column<String?> = text("role").nullable()
    val genre: Column<String?> = text("genre").nullable()
    val biography: Column<String?> = text("biography").nullable()
    val walletAddress: Column<String?> = text("wallet_address").nullable()
    val email: Column<String> = text("email")
    val passwordHash: Column<String?> = text("password_hash").nullable()
    val verificationStatus: Column<UserVerificationStatus> =
        enumeration("verification_status", UserVerificationStatus::class).default(UserVerificationStatus.Unverified)
    val companyName: Column<String?> = text("company_name").nullable()
    val companyLogoUrl: Column<String?> = text("company_logo_url").nullable()
    val companyIpRights: Column<Boolean?> = bool("company_ip_rights").nullable()
    val isni: Column<String?> = text("isni").nullable()
    val ipi: Column<String?> = text("ipi").nullable()
    val dspPlanSubscribed: Column<Boolean> = bool("dsp_plan_subscribed").default(false)
    val admin: Column<Boolean> = bool("admin").default(false)
    val distributionUserId: Column<String?> = text("distribution_user_id").nullable()
    val distributionArtistId: Column<Long?> = long("distribution_artist_id").nullable()
    val distributionParticipantId: Column<Long?> = long("distribution_participant_id").nullable()
    val distributionSubscriptionId: Column<Long?> = long("distribution_subscription_id").nullable()
    val distributionLabelId: Column<Long?> = long("distribution_label_id").nullable()
    val distributionIsni: Column<String?> = text("distribution_isni").nullable()
    val distributionIpn: Column<String?> = text("distribution_ipn").nullable()
    val distributionNewmParticipantId: Column<Long?> = long("distribution_newm_participant_id").nullable()
    val referralStatus: Column<ReferralStatus> =
        enumeration("referral_status", ReferralStatus::class).default(ReferralStatus.NotReferred)
    val referralCode: Column<String?> = text("referral_code").nullable()
}
