package io.newm.server.features.referralhero.repo

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.retry
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.server.application.ApplicationEnvironment
import io.newm.server.features.referralhero.model.CommonResponse
import io.newm.server.ktx.checkedBody
import io.newm.server.ktx.getSecureConfigString
import io.newm.shared.ktx.coLazy
import io.newm.shared.ktx.getConfigString

class ReferralHeroRepositoryImpl(
    environment: ApplicationEnvironment,
    private val httpClient: HttpClient,
) : ReferralHeroRepository {
    private val logger = KotlinLogging.logger {}
    private val subscribersUrl: String by coLazy {
        val apiUrl = environment.getConfigString("referralHero.apiUrl")
        val campaignId = environment.getSecureConfigString("referralHero.referralCampaignId")
        "$apiUrl/lists/$campaignId/subscribers"
    }
    private val apiToken: String by coLazy {
        environment.getSecureConfigString("referralHero.apiToken")
    }

    override suspend fun addSubscriber(
        email: String,
        referrer: String?
    ): String? {
        val response: CommonResponse = httpClient
            .post(subscribersUrl) {
                commonParamsAndOptions(email)
                referrer?.let {
                    parameter("referrer", it)
                    parameter("status", "custom_event_pending")
                }
            }.checkedBody()
        return if (response.isStatusOk) {
            logger.debug { "Succeeded add-subscriber for $email" }
            response.data?.code
        } else {
            logger.error { "Failed add-subscriber for $email - response: $response" }
            null
        }
    }

    override suspend fun trackReferralConversion(email: String): Boolean {
        val response: CommonResponse = httpClient
            .post("$subscribersUrl/track_referral_conversion_event") {
                commonParamsAndOptions(email)
            }.checkedBody()
        return if (response.isStatusOk) {
            logger.debug { "Succeeded track-referral-conversion for $email" }
            true
        } else {
            logger.error { "Failed track-referral-conversion for $email - response: $response" }
            false
        }
    }

    override suspend fun confirmReferral(email: String): Boolean {
        val response: CommonResponse = httpClient
            .post("$subscribersUrl/confirm") {
                commonParamsAndOptions(email)
            }.checkedBody()
        return if (response.isStatusOk) {
            logger.debug { "Succeeded confirm-referral for $email" }
            true
        } else {
            logger.error { "Failed confirm-referral for $email - response: $response" }
            false
        }
    }

    private fun HttpRequestBuilder.commonParamsAndOptions(email: String) {
        parameter("email", email)
        parameter("api_token", apiToken)
        retry {
            maxRetries = 2
            delayMillis { 500L }
        }
    }
}
