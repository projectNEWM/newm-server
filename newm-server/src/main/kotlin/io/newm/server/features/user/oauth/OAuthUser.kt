package io.newm.server.features.user.oauth

interface OAuthUser {
    val id: String
    val firstName: String?
    val lastName: String?
    val pictureUrl: String?
    val email: String?
    val isEmailVerified: Boolean?
}
