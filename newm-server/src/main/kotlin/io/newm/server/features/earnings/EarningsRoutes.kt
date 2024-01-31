package io.newm.server.features.earnings

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import io.newm.chain.util.extractStakeAddress
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.auth.jwt.AUTH_JWT_ADMIN
import io.newm.server.features.cardano.repo.CardanoRepository
import io.newm.server.features.earnings.repo.EarningsRepository
import io.newm.shared.koin.inject
import io.newm.shared.ktx.get
import io.newm.shared.ktx.post

private const val EARNINGS_PATH = "v1/earnings"

fun Routing.createEarningsRoutes() {
    val cardanoRepository: CardanoRepository by inject()
    val earningsRepository: EarningsRepository by inject()

    authenticate(AUTH_JWT_ADMIN) {
        route(EARNINGS_PATH) {
            // create earning records
            post {
                TODO()
            }
        }
    }
    authenticate(AUTH_JWT) {
        route("$EARNINGS_PATH/{walletAddress}") {
            // get earnings
            get {
                val stakeAddress = parameters["walletAddress"]!!.extractStakeAddress(cardanoRepository.isMainnet())
                val earnings = earningsRepository.getAllByStakeAddress(stakeAddress)
                TODO()
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
