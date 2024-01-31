package io.newm.server.auth.twofactor.repo

interface TwoFactorAuthRepository {
    suspend fun sendCode(email: String)

    suspend fun verifyCode(
        email: String,
        code: String
    ): Boolean
}
