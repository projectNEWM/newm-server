package io.newm.server.features.marketplace

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import io.newm.server.features.marketplace.model.saleFilters
import io.newm.server.features.marketplace.repo.MarketplaceRepository
import io.newm.server.features.model.CountResponse
import io.newm.server.ktx.limit
import io.newm.server.ktx.offset
import io.newm.server.ktx.saleId
import io.newm.server.recaptcha.repo.RecaptchaRepository
import io.newm.shared.ktx.get
import io.newm.shared.ktx.post
import org.koin.ktor.ext.inject

private const val ROOT_PATH = "v1/marketplace"

@Suppress("unused")
fun Routing.createMarketplaceRoutes() {
    val recaptchaRepository: RecaptchaRepository by inject()
    val marketplaceRepository: MarketplaceRepository by inject()

    route(ROOT_PATH) {
        route("sales") {
            get {
                // TODO: re-enable when WebApp fixed
                // recaptchaRepository.verify("get_sales", request)
                respond(marketplaceRepository.getSales(saleFilters, offset, limit))
            }
            get("count") {
                // TODO: re-enable when WebApp fixed
                // recaptchaRepository.verify("get_sale_count", request)
                respond(CountResponse(marketplaceRepository.getSaleCount(saleFilters)))
            }
            get("{saleId}") {
                // TODO: re-enable when WebApp fixed
                // recaptchaRepository.verify("get_sale", request)
                respond(marketplaceRepository.getSale(saleId))
            }
        }
        route("orders") {
            post("amount") {
                recaptchaRepository.verify("generate_order_amount", request)
                respond(marketplaceRepository.generateOrderAmount(receive()))
            }
            post("transaction") {
                recaptchaRepository.verify("generate_order_transaction", request)
                respond(marketplaceRepository.generateOrderTransaction(receive()))
            }
        }
    }
}