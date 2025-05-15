package io.newm.server.features.referralhero.repo

import io.newm.server.features.referralhero.model.ReferralHeroSubscriber

interface ReferralHeroRepository {
    suspend fun getOrCreateSubscriber(
        email: String,
        referrer: String? = null,
    ): ReferralHeroSubscriber?

    suspend fun trackReferralConversion(email: String): Boolean

    suspend fun confirmReferral(email: String): Boolean
}
