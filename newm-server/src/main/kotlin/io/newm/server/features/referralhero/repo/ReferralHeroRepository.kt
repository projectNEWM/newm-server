package io.newm.server.features.referralhero.repo

interface ReferralHeroRepository {
    suspend fun addSubscriber(
        email: String,
        referrer: String? = null,
    ): String?

    suspend fun trackReferralConversion(email: String): Boolean

    suspend fun confirmReferral(email: String): Boolean
}
