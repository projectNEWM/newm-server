package io.newm.server.features.paypal

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.features.paypal.repo.PayPalRepository
import io.newm.server.ktx.myUserId
import io.newm.shared.koin.inject
import io.newm.shared.ktx.post

fun Routing.createPayPalRoutes() {
    val repository: PayPalRepository by inject()

    authenticate(AUTH_JWT) {
        route("v1/paypal/minting-distribution/orders") {
            post {
                respond(HttpStatusCode.Created, repository.createMintingDistributionOrder(myUserId, receive()))
            }
            post("{orderId}/capture") {
                repository.captureMintingDistributionOrder(parameters["orderId"]!!)
                respond(HttpStatusCode.OK)
            }
        }
    }
}
