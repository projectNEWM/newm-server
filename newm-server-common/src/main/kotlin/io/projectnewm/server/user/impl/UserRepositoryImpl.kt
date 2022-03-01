package io.projectnewm.server.user.impl

import io.projectnewm.server.oauth.OAuthType
import io.projectnewm.server.user.UserRepository
import java.util.UUID

internal class UserRepositoryImpl : UserRepository {

    @Suppress("UNUSED_PARAMETER")
    override suspend fun registerUser(oauthType: OAuthType, accessToken: String): String {
        // TODO: implement
        return UUID.randomUUID().toString()
    }
}
