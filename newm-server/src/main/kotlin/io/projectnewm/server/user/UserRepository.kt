package io.projectnewm.server.user

import io.projectnewm.server.pugins.auth.OAuthType
import java.util.UUID

class UserRepository {

    @Suppress("UNUSED_PARAMETER")
    suspend fun registerUser(oauthType: OAuthType, accessToken: String): String {
        // TODO: implement
        return UUID.randomUUID().toString()
    }
}
