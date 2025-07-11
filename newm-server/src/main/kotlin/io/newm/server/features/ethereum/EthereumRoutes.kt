package io.newm.server.features.ethereum

import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.newm.server.auth.jwt.AUTH_JWT
import io.newm.server.features.ethereum.repo.EthereumRepository
import io.newm.server.ktx.requiredQueryParam
import io.newm.shared.koin.inject
import io.newm.shared.ktx.get

fun Routing.createEthereumRoutes() {
    val repository: EthereumRepository by inject()

    authenticate(AUTH_JWT) {
        get("/v1/ethereum/nft/songs") {
            // TODO: for now, during the initial experimentation phase, we get the owner address directly from
            //  the client, later we will migrate to a Wallet connection model similar to Cardano
            respond(repository.getNftSongs(request.requiredQueryParam("ownerAddress")))
        }
    }
}
