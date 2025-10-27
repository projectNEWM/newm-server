package io.newm.server.features.referralhero.repo

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.retry
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.server.application.ApplicationEnvironment
import io.newm.chain.util.extractStakeAddress
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_REFERRAL_HERO_REWARD_USD
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.earnings.model.Earning
import io.newm.server.features.earnings.repo.EarningsRepository
import io.newm.server.features.referralhero.model.CommonResponse
import io.newm.server.features.referralhero.model.ReferralHeroSubscriber
import io.newm.server.features.referralhero.model.ReferralStatus.Confirmed
import io.newm.server.features.referralhero.model.ReferralStatus.NotReferred
import io.newm.server.features.referralhero.model.ReferralStatus.Pending
import io.newm.server.features.referralhero.model.ReferralStatus.Unconfirmed
import io.newm.server.features.user.database.UserEntity
import io.newm.server.ktx.checkedBody
import io.newm.server.ktx.getSecureConfigString
import io.newm.shared.ktx.coLazy
import io.newm.shared.ktx.getConfigString
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

class ReferralHeroRepositoryImpl(
    environment: ApplicationEnvironment,
    private val httpClient: HttpClient,
    private val configRepository: ConfigRepository,
    private val earningsRepository: EarningsRepository,
    private val cardanoRepository: CardanoRepository
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

    override suspend fun getOrCreateSubscriber(
        email: String,
        referrer: String?
    ): ReferralHeroSubscriber? {
        val response: CommonResponse = httpClient
            .post(subscribersUrl) {
                commonParamsAndOptions(email)
                referrer?.let {
                    parameter("referrer", it)
                    parameter("status", "custom_event_pending")
                }
            }.checkedBody()
        return if (response.isStatusOk) {
            logger.debug { "Succeeded get-or-create-subscriber for $email" }
            response.data?.let {
                ReferralHeroSubscriber(
                    referralCode = it.code,
                    referralStatus = when (it.referralStatus) {
                        "pending" -> Pending
                        "unconfirmed" -> Unconfirmed
                        "confirmed" -> Confirmed
                        else -> NotReferred
                    }
                )
            }
        } else {
            logger.error { "Failed get-or-create-subscriber for $email - response: $response" }
            null
        }
    }

    override suspend fun trackReferralConversion(email: String): Boolean {
        val response: CommonResponse = httpClient
            .post("$subscribersUrl/track_referral_conversion_event") {
                commonParamsAndOptions(email)
            }.checkedBody()
        return if (response.isStatusOk && response.data?.response == "custom_event_completed") {
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

        if (!response.isStatusOk || response.data?.response != "subscriber_confirmed") {
            logger.error { "Failed confirm-referral for $email - response: $response" }
            return false
        }
        logger.debug { "Succeeded confirm-referral for $email" }

        val (referee, referrer) = transaction {
            UserEntity.getByEmail(email) to response.data
                ?.referredBy
                ?.email
                ?.let { UserEntity.getByEmail(it) }
        }
        checkNotNull(referee) { "Referee $email not found" }
        checkNotNull(referrer) { "Referrer for $email not found" }

        val usdAmount = configRepository.getLong(CONFIG_KEY_REFERRAL_HERO_REWARD_USD)
        val newmUsdPrice = cardanoRepository.queryNEWMUSDPrice()
        val newmAmount = (usdAmount.toBigDecimal() / newmUsdPrice.toBigDecimal()).movePointRight(6).toLong()
        val newmUsdPriceStr = newmUsdPrice.toBigDecimal().movePointLeft(6).toPlainString()
        val memo = "Referral reward for: ${referee.stageOrFullName} @ Ɲ1 = $$newmUsdPriceStr"
        val now = LocalDateTime.now()

        referee.walletAddress?.let {
            logger.info { "Referee reward: Ɲ$newmAmount to ${referee.id}" }
            earningsRepository.add(
                Earning(
                    stakeAddress = it.extractStakeAddress(cardanoRepository.isMainnet()),
                    amount = newmAmount,
                    memo = memo,
                    createdAt = now,
                    startDate = now
                )
            )
        } ?: run {
            logger.warn { "Referee ${referee.id} doesn't have a wallet address - skipping reward earnings" }
        }

        referrer.walletAddress?.let {
            logger.info { "Referrer reward: Ɲ$newmAmount to ${referrer.id}" }
            earningsRepository.add(
                Earning(
                    stakeAddress = it.extractStakeAddress(cardanoRepository.isMainnet()),
                    amount = newmAmount,
                    memo = memo,
                    createdAt = now,
                    startDate = now
                )
            )
        } ?: run {
            logger.warn { "Referrer ${referrer.id} doesn't have a wallet address - skipping reward earnings" }
        }
        return true
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
