package io.newm.server.features.earnings

import com.google.iot.cbor.CborInteger
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import io.newm.chain.util.extractStakeAddress
import io.newm.chain.util.toHexString
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.auth.jwt.AUTH_JWT_ADMIN
import io.newm.server.config.repo.ConfigRepository
import io.newm.server.config.repo.ConfigRepository.Companion.CONFIG_KEY_EARNINGS_CLAIM_ORDER_FEE
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.earnings.model.AddSongRoyaltyRequest
import io.newm.server.features.earnings.model.ClaimOrderRequest
import io.newm.server.features.earnings.model.Earning
import io.newm.server.features.earnings.model.GetEarningsResponse
import io.newm.server.features.earnings.repo.EarningsRepository
import io.newm.server.ktx.songId
import io.newm.server.recaptcha.repo.RecaptchaRepository
import io.newm.shared.koin.inject
import io.newm.shared.ktx.get
import io.newm.shared.ktx.post

private const val EARNINGS_PATH = "v1/earnings"
private const val EARNINGS_PATH_ADMIN = "v1/earnings/admin"

fun Routing.createEarningsRoutes() {
    val configRepository: ConfigRepository by inject()
    val cardanoRepository: CardanoRepository by inject()
    val earningsRepository: EarningsRepository by inject()
    val recaptchaRepository: RecaptchaRepository by inject()

    authenticate(AUTH_JWT_ADMIN) {
        route(EARNINGS_PATH_ADMIN) {
            get {
                val earnings = earningsRepository.getAll()
                respond(earnings)
            }
            // create earning records
            post {
                val earnings = receive<List<Earning>>()
                earningsRepository.addAll(earnings)
                respond(HttpStatusCode.Created)
            }
            get("{songId}") {
                // get earnings by song id
                val earnings = earningsRepository.getAllBySongId(songId)
                respond(earnings)
            }
            post("{songId}") {
                // create earning records for a song based on receiving a total amount of royalties.
                val royaltyRequest: AddSongRoyaltyRequest = receive()
                earningsRepository.addRoyaltySplits(songId, royaltyRequest)
                respond(HttpStatusCode.Created)
            }
        }
    }

    // Claiming is un-authenticated, but we still check recaptcha to prevent bots
    authenticate(AUTH_JWT, optional = true) {
        route(EARNINGS_PATH) {
            // get earnings
            get("{walletAddress}") {
                val stakeAddress = parameters["walletAddress"]!!.extractStakeAddress(cardanoRepository.isMainnet())
                recaptchaRepository.verify("get_earnings", request)
                val earnings = earningsRepository.getAllByStakeAddress(stakeAddress)
                val totalClaimed = earnings.filter { it.claimed }.sumOf { it.amount }
                val paymentAmountLovelace = configRepository.getLong(CONFIG_KEY_EARNINGS_CLAIM_ORDER_FEE)
                val changeAmountLovelace = 1000000L // 1 ada
                val amountCborHex = CborInteger
                    .create(paymentAmountLovelace + changeAmountLovelace)
                    .toCborByteArray()
                    .toHexString()
                respond(
                    GetEarningsResponse(
                        totalClaimed = totalClaimed,
                        earnings = earnings,
                        amountCborHex = amountCborHex,
                    )
                )
            }
            // create a claim for all earnings on this wallet stake address
            post {
                val claimOrderRequest = receive<ClaimOrderRequest>()
                recaptchaRepository.verify("post_earnings", request)
                val claimOrder = earningsRepository.createClaimOrder(claimOrderRequest)

                if (claimOrder != null) {
                    respond(claimOrder)
                } else {
                    respond(HttpStatusCode.NotFound, "No unclaimed earnings found for this wallet address.")
                }
            }
        }
    }
}
