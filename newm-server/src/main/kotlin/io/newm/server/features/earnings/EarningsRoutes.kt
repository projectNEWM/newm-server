package io.newm.server.features.earnings

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.newm.chain.util.extractStakeAddress
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.auth.jwt.AUTH_JWT_ADMIN
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.earnings.model.AddSongRoyaltyRequest
import io.newm.server.features.earnings.model.Earning
import io.newm.server.features.earnings.repo.EarningsRepository
import io.newm.shared.koin.inject
import io.newm.shared.ktx.get
import io.newm.shared.ktx.post
import io.newm.shared.ktx.toUUID

private const val EARNINGS_PATH = "v1/earnings"

fun Routing.createEarningsRoutes() {
    val cardanoRepository: CardanoRepository by inject()
    val earningsRepository: EarningsRepository by inject()

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
    authenticate(AUTH_JWT) {
        route("$EARNINGS_PATH/{walletAddress}") {
            // get earnings
            get {
                val stakeAddress = parameters["walletAddress"]!!.extractStakeAddress(cardanoRepository.isMainnet())
                val earnings = earningsRepository.getAllByStakeAddress(stakeAddress)
                respond(earnings)
            }
            // create a claim for all earnings on this wallet stake address
            post {
                val stakeAddress = parameters["walletAddress"]!!.extractStakeAddress(cardanoRepository.isMainnet())
                // check for existing open claim record first
                // TODO
                // create a new claim record
                TODO()
            }
        }
    }
}
