package io.newm.server.features.paypal

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.features.paypal.model.MintingDistributionOrderRequest
import io.newm.server.features.paypal.repo.PayPalRepository
import io.newm.server.ktx.myUserId
import io.newm.shared.koin.inject
import io.newm.shared.ktx.post

fun Routing.createPayPalRoutes() {
    val repository: PayPalRepository by inject()
    val logger = KotlinLogging.logger {}

    authenticate(AUTH_JWT) {
        route("v1/paypal/minting-distribution/orders") {
            post {
                val request = receive<MintingDistributionOrderRequest>()
                try {
                    respond(HttpStatusCode.Created, repository.createMintingDistributionOrder(myUserId, request))
                } catch (e: Exception) {
                    logger.error(e) { "PayPal order creation failed for songId={${request.songId}}" }
                    throw e
                }
            }
            post("{orderId}/capture") {
                val orderId = parameters["orderId"]!!
                try {
                    repository.captureMintingDistributionOrder(orderId)
                    respond(HttpStatusCode.OK)
                } catch (e: Exception) {
                    logger.error(e) { "PayPal order capture failed for orderId=$orderId" }
                    throw e
                }
            }
        }
    }
}
