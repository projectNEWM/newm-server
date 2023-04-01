package io.newm.server.features.user.database

import io.newm.server.auth.oauth.OAuthType
import io.newm.server.features.user.model.UserVerificationStatus
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object UserTable : UUIDTable(name = "users") {
    val createdAt: Column<LocalDateTime> = datetime("created_at").defaultExpression(CurrentDateTime)
    val oauthType: Column<OAuthType?> = enumeration("oauth_type", OAuthType::class).nullable()
    val oauthId: Column<String?> = text("oauth_id").nullable()
    val firstName: Column<String?> = text("first_name").nullable()
    val lastName: Column<String?> = text("last_name").nullable()
    val nickname: Column<String?> = text("nickname").nullable()
    val pictureUrl: Column<String?> = text("picture_url").nullable()
    val bannerUrl: Column<String?> = text("banner_url").nullable()
    val websiteUrl: Column<String?> = text("website_url").nullable()
    val twitterUrl: Column<String?> = text("twitter_url").nullable()
    val instagramUrl: Column<String?> = text("instagram_url").nullable()
    val location: Column<String?> = text("location").nullable()
    val role: Column<String?> = text("role").nullable()
    val genre: Column<String?> = text("genre").nullable()
    val biography: Column<String?> = text("biography").nullable()
    val walletAddress: Column<String?> = text("wallet_address").nullable()
    val email: Column<String> = text("email")
    val passwordHash: Column<String?> = text("password_hash").nullable()
    val verificationStatus: Column<UserVerificationStatus> =
        enumeration("verification_status", UserVerificationStatus::class)
            .default(UserVerificationStatus.Unverified)
    val companyName: Column<String?> = text("company_name").nullable()
    val companyLogoUrl: Column<String?> = text("company_logo_url").nullable()
    val companyIpRights: Column<Boolean?> = bool("company_ip_rights").nullable()
}
