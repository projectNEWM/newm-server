package io.projectnewm.server.features.user.model

import io.projectnewm.server.auth.oauth.OAuthType
import io.projectnewm.server.auth.password.Password
import io.projectnewm.server.serialization.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class User(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID? = null,
    val oauthType: OAuthType? = null,
    val oauthId: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val nickname: String? = null,
    val pictureUrl: String? = null,
    val role: String? = null,
    val genre: String? = null,
    val email: String? = null,
    val newPassword: Password? = null,
    val confirmPassword: Password? = null,
    val currentPassword: Password? = null,
    val authCode: String? = null
)
