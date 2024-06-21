package io.newm.server.features.earnings

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import io.newm.chain.util.extractStakeAddress
import io.newm.server.auth.jwt.AUTH_JWT_ADMIN
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.earnings.model.AddSongRoyaltyRequest
import io.newm.server.features.earnings.model.Earning
import io.newm.server.features.earnings.repo.EarningsRepository
import io.newm.server.features.user.database.UserTable.walletAddress
import io.newm.server.recaptcha.repo.RecaptchaRepository
import io.newm.shared.koin.inject
import io.newm.shared.ktx.get
import io.newm.shared.ktx.post
import io.newm.shared.ktx.toUUID

private const val EARNINGS_PATH = "v1/earnings"

fun Routing.createEarningsRoutes() {
    val cardanoRepository: CardanoRepository by inject()
    val earningsRepository: EarningsRepository by inject()
    val recaptchaRepository: RecaptchaRepository by inject()

    authenticate(AUTH_JWT_ADMIN) {
        route(EARNINGS_PATH) {
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
        }
        get("$EARNINGS_PATH/{songId}") {
            // get earnings by song id
            val songId = parameters["songId"]!!.toUUID()
            val earnings = earningsRepository.getAllBySongId(songId)
            respond(earnings)
        }
        post("$EARNINGS_PATH/{songId}") {
            // create earning records for a song based on receiving a total amount of royalties.
            val songId = parameters["songId"]!!.toUUID()
            val royaltyRequest: AddSongRoyaltyRequest = receive()
            earningsRepository.addRoyaltySplits(songId, royaltyRequest)
            respond(HttpStatusCode.Created)
        }
    }

    // Claiming is un-authenticated, but we still check recaptcha to prevent bots
    route("$EARNINGS_PATH/{walletAddress}") {
        // get earnings
        get {
            recaptchaRepository.verify("getearnings_$walletAddress", request)
            val stakeAddress = parameters["walletAddress"]!!.extractStakeAddress(cardanoRepository.isMainnet())
            val earnings = earningsRepository.getAllByStakeAddress(stakeAddress)
            respond(earnings)
        }
        // create a claim for all earnings on this wallet stake address
        post {
            recaptchaRepository.verify("postearnings_$walletAddress", request)
            val stakeAddress = parameters["walletAddress"]!!.extractStakeAddress(cardanoRepository.isMainnet())
            val claimOrder = earningsRepository.createClaimOrder(stakeAddress)

            if (claimOrder != null) {
                respond(claimOrder)
            } else {
                respond(HttpStatusCode.NotFound, "No unclaimed earnings found for this wallet address.")
            }
        }
    }
}
