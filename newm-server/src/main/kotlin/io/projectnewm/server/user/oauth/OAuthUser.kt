package io.projectnewm.server.user.oauth

interface OAuthUser {
    val id: String
    val firstName: String?
    val lastName: String?
    val pictureUrl: String?
    val email: String?
}
