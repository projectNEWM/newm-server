package io.projectnewm.server.auth.twofactor

interface TwoFactorAuthRepository {
    suspend fun sendCode(email: String)
    suspend fun verifyCode(email: String, code: String): Boolean
}
